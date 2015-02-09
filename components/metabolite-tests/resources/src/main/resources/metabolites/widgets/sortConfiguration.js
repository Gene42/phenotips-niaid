/**
 * This widget allows multi select elements (for now just lists of HTML checkbox elements) to be dragged and dropped.
 * It manages 2 lists: one with all available options and the other which contains the actual configuration (order).
 * Elements in the available list could optionally be marked with class 'permanent-option' which will then never
 * disappear from the options list.
 * The 2 lists must be created prior to the script running. The input into which the order will be serialized to must
 * also be created prior to running the script.
 */
require(['jquery', 'resources/js/amd/jquery-ui/jquery-ui'], function ($)
{
    window.PhenoTips = (function (PhenoTips)
    {
        var widgets = PhenoTips.widgets = PhenoTips.widgets || {};

        widgets.SortConfiguration = {
            permanentClass: 'permanent-option',
            optionsClass: 'sort-configuration-options',
            configurationClass: 'sort-configuration',
            initialize: function ()
            {
                var _this = this;
                var configurationHolder = $('.sort-dd-configuration-holder');
                var sortableLists = configurationHolder.find('ul.sortable');
                var storageInput = configurationHolder.children('input');
                var sortConfigContainer = sortableLists.filter('ul.' + this.configurationClass);
                var sortOptionsContainer = sortableLists.filter('ul.' + this.optionsClass);

                // used to determine if clicks to move from options into config are allowed at the moment
                var statusTracker = {isDragged: false};
                var storeFunction = _this.store(storageInput, sortConfigContainer);
                var managePermanentFunction = _this.managePermanent(sortOptionsContainer);

                this.clickMove(statusTracker, sortConfigContainer, sortOptionsContainer, storeFunction);
                sortableLists.sortable({
                    connectWith: '.sort-configuration-connected',
                    out: managePermanentFunction,
                    deactivate: function(event, ui) {
                        managePermanentFunction(event, ui);
                    },
                    update: storeFunction,
                    activate: _this.disableClick(statusTracker),
                    distance: 5,
                    forceHelperSize: true,
                    forcePlaceholderSize: true,
                    helper: "clone",
                    placeholder: 'placeholder'
                });
                sortConfigContainer.data('uiSortable').floating = true;
                sortableLists.disableSelection();
                // to prevent attaching a listener to the document (onLoad)
                return true;
            },
            getPermanentOption: function (holder)
            {
                var optionsList = holder.find('.' + this.optionsClass);
                return optionsList.children("li." + this.permanentClass);
            },
            /**
             * Listener preventing permanent options from being removed, and from having more than one permanent option.
             */
            managePermanent: function (optionsContainer)
            {
                var _this = this;
                return function (event, ui)
                {
                    if (ui.sender) {
                        if (ui.sender.hasClass(_this.optionsClass) &&
                            ui.sender.children('.' + _this.permanentClass).length == 0)
                        {
                            _this.cloneInto(ui.item, ui.sender);
                        } else if (ui.item.hasClass(_this.permanentClass) && !ui.sender.hasClass(_this.optionsClass) &&
                            optionsContainer.children('.' + _this.permanentClass).length > 0 &&
                            !ui.item.parent().is(ui.sender))
                        {
                            console.log('remove');
                            console.log(ui.item.parent());
                            console.log(!ui.item.parent().is(ui.sender), ui.sender);
                            ui.item.remove();
                        }
                    }
                }
            },
            cloneInto: function (elem, to)
            {
                var cloned = elem.clone();
                cloned.css({'top': 'auto', 'left': 'auto', 'right': 'auto', 'bottom': 'auto'});
                to.append(cloned);
            },
            /**
             * Serializes the order of the elements, and stores them into an input.
             */
            store: function (input, container)
            {
                var _this = this;
                return function ()
                {
                    var serialized = "";
                    container.children('li').each(function (i, elem)
                    {
                        serialized += $(elem).children(".sort-option-value").html() + ","
                    });
                    input.val(serialized);
                    console.log(serialized)
                }
            },
            /**
             * Attaches a listener to allow for clicking to move items from the options list to the configuration list.
             * Makes sure that the moving of elements between lists conforms to expectations.
             * Must be used with disableClick.
             */
            clickMove: function (status, configContainer, optionsContainer, store)
            {
                var _this = this;
                configContainer.on('click', 'li', function (event)
                {
                    if (!status.isDragged) {
                        console.log("config yes");
                        var target = $(event.target);
                        if (target.hasClass(_this.permanentClass) &&
                            optionsContainer.children('.' + _this.permanentClass).length > 0)
                        {
                            target.remove();
                            return;
                        }
                        optionsContainer.append(event.target);
                        store();
                    } else {
                        console.log("config no");
                        status.isDragged = false;
                    }
                });
                optionsContainer.on('click', 'li', function (event)
                {
                    var target = $(event.target);
                    if (!status.isDragged) {
                        console.log("options yes");
                        if (target.hasClass(_this.permanentClass)) {
                            _this.cloneInto(target, configContainer);
                        } else {
                            configContainer.append(target);
                        }
                        store();
                    } else {
                        console.log("options no");
                        status.isDragged = false;
                    }
                });
            },
            disableClick: function (status)
            {
                return function ()
                {
                    status.isDragged = true;
                }
            }
        };

        return PhenoTips;
    }(PhenoTips || {}));

    (XWiki.domIsLoaded && window.PhenoTips.widgets.SortConfiguration.initialize()) ||
    $(document).ready(window.PhenoTips.widgets.SortConfiguration.initialize)
});

