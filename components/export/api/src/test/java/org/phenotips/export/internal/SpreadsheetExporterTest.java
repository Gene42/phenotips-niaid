/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.export.internal;

import org.phenotips.data.Patient;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SpreadsheetExporterTest
{
    @Test
    public void badParameters() throws Exception
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        OutputStream stream = mock(OutputStream.class);

        spy.export(null, null, stream);
        verify(spy, times(0)).processMainSheet(anySet(), anyList());
        spy.export(new String[1], null, null);
        verify(spy, times(0)).processMainSheet(anySet(), anyList());
    }

    @Test(expected = Exception.class)
    public void failTest() throws Exception
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        OutputStream stream = mock(OutputStream.class);
        Workbook workbook = mock(Workbook.class);

        doThrow(Exception.class).when(workbook).write(stream);
        when(spy.createNewWorkbook()).thenReturn(workbook);
        doNothing().when(spy).processMainSheet(anySet(), anyList());

        List<Patient> list = new LinkedList<>();
        spy.export(new String[0], list, stream);
        verify(stream, atLeastOnce()).close();
    }

    @Test
    public void exportTest() throws Exception
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        OutputStream stream = mock(OutputStream.class);
        Workbook workbook = mock(Workbook.class);

        when(spy.createNewWorkbook()).thenReturn(workbook);
        doNothing().when(spy).processMainSheet(anySet(), anyList());

        List<Patient> list = new LinkedList<>();
        spy.export(new String[0], list, stream);
        verify(workbook, times(1)).write(stream);
        verify(stream, times(1)).flush();
    }

    @Test
    public void processMainSheet() throws Exception
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Workbook workbook = mock(Workbook.class);
        SheetAssembler assembler = mock(SheetAssembler.class);
        Sheet sheet = mock(Sheet.class);

        spy.wBook = workbook;
        doReturn(sheet).when(workbook).createSheet(anyString());
        doReturn(assembler).when(spy).runAssembler(anySet(), anyList());
        doNothing().when(spy).commit(any(DataSection.class), any(Sheet.class));
        doNothing().when(spy).freezeHeader(anyShort(), any(Sheet.class));

        spy.processMainSheet(anySet(), anyList());

        Assert.assertTrue(exporter.sheets.containsValue(sheet));
        verify(spy, atLeastOnce()).commit(any(DataSection.class), any(Sheet.class));
        verify(spy, atLeastOnce()).freezeHeader(anyShort(), any(Sheet.class));
    }

    @Test
    public void emptyCommit()
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Sheet sheet = mock(Sheet.class);
        DataSection section = mock(DataSection.class);

        doNothing().when(spy).commitRows(eq(section), eq(sheet), any(Styler.class));
        doReturn(-1).when(section).getMaxX();
        doReturn(-1).when(section).getMaxY();

        spy.commit(section, sheet);
        verifyNoMoreInteractions(sheet);
    }

    @Test
    public void commitTest()
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Sheet sheet = mock(Sheet.class);
        DataSection section = mock(DataSection.class);
        DataCell cell = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[1][1];
        matrix[0][0] = cell;
        InOrder order = inOrder(sheet, sheet);

        doNothing().when(spy).commitRows(eq(section), eq(sheet), any(Styler.class));
        doReturn(0).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();
        doReturn(matrix).when(section).getMatrix();
        doReturn(1).when(cell).getMergeX();
        doReturn(DataToCellConverter.charactersPerLine * 210 + 1).when(sheet).getColumnWidth(anyInt());

        spy.commit(section, sheet);
        order.verify(sheet, atLeastOnce()).autoSizeColumn(anyInt());
        verify(sheet, atLeastOnce()).setColumnWidth(anyInt(), anyInt());
        order.verify(sheet).addMergedRegion(any(CellRangeAddress.class));
    }

    @Test
    public void commitRowTest()
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cell = mock(Cell.class);
        DataSection section = mock(DataSection.class);
        Styler styler = mock(Styler.class);
        DataCell dataCell = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[1][1];
        matrix[0][0] = dataCell;

        doReturn(matrix).when(section).getMatrix();
        doReturn(row).when(sheet).createRow(anyInt());
        doReturn(0).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();
        doReturn(cell).when(row).createCell(anyInt());
        doNothing().when(cell).setCellValue(anyString());
        doNothing().when(styler).style(any(DataCell.class), any(Cell.class), any(Workbook.class));
        doReturn(0).when(dataCell).getNumberOfLines();

        spy.commitRows(section, sheet, styler);

        verify(sheet, times(1)).createRow(anyInt());
        verify(row, times(1)).createCell(anyInt());
        verify(cell, times(1)).setCellValue(anyString());
        verify(styler, times(1)).style(any(DataCell.class), any(Cell.class), any(Workbook.class));
        verify(row, atMost(0)).setHeight(anyShort());
    }

    @Test
    public void commitEmptyTest()
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        DataSection section = mock(DataSection.class);
        Styler styler = mock(Styler.class);
        DataCell[][] matrix = new DataCell[1][1];

        doReturn(matrix).when(section).getMatrix();
        doReturn(row).when(sheet).createRow(anyInt());
        doReturn(0).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();

        spy.commitRows(section, sheet, styler);

        verifyZeroInteractions(row);
        verifyZeroInteractions(styler);
    }

    @Test
    public void commitRowAdjustHeightTest()
    {
        SpreadsheetExporter exporter = new SpreadsheetExporter();
        SpreadsheetExporter spy = spy(exporter);
        Sheet sheet = mock(Sheet.class);
        Row row = mock(Row.class);
        Cell cell = mock(Cell.class);
        DataSection section = mock(DataSection.class);
        Styler styler = mock(Styler.class);
        DataCell dataCell = mock(DataCell.class);
        DataCell[][] matrix = new DataCell[1][1];
        matrix[0][0] = dataCell;

        doReturn(matrix).when(section).getMatrix();
        doReturn(row).when(sheet).createRow(anyInt());
        doReturn(0).when(section).getMaxX();
        doReturn(0).when(section).getMaxY();
        doReturn(cell).when(row).createCell(anyInt());
        doNothing().when(cell).setCellValue(anyString());
        doNothing().when(styler).style(any(DataCell.class), any(Cell.class), any(Workbook.class));
        doReturn(2).when(dataCell).getNumberOfLines();

        spy.commitRows(section, sheet, styler);

        verify(row, times(1)).setHeight(anyShort());
    }
}
