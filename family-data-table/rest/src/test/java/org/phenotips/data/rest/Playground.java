package org.phenotips.data.rest;

import java.net.URI;
import java.net.URL;

import org.junit.Test;

/**
 * DESCRIPTION.
 *
 * @version $Id$
 */
public class Playground
{

    @Test
    public void test1() throws Exception {
        String str = "http://localhost:8080/export/data/P0000004?format=xar&amp;name=xwiki:data.P0000004&amp;pages=xwiki:data.P0000004";

        URL url = new URL(str);
        URI uri = new URI(str);

        System.out.println("url=" + url.getPath() + url.getQuery());
        System.out.println("uri=" + uri.getPath());
    }
}
