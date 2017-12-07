package dk.statsbiblioteket.summa.plugins;

import net.sf.saxon.value.StringValue;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.*;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
public class SaxonXSLTTest {

    @Test
    public void testDateTimeFormatDefinitionLogic() throws ParseException {
        assertEquals("2017-12-07T11:51:00", Datetime.isoToCustom(
                             "2017-12-07T10:51:00Z", "YYYY-MM-dd'T'HH:mm:ss", "Europe/Copenhagen"));
        assertEquals("2017-12", Datetime.isoToCustom(
                             "2017-12-07T10:51:00Z", "YYYY-MM", "Europe/Copenhagen"));
    }

}