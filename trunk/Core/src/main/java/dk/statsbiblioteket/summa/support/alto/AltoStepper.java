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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Steps through an {@link Alto} object and provides callbacks throughout the structure.
 * </p><p>
 * Override the relevant process-methods. null is accepted as return value and will result in the given object being
 * removed from the overall Alto-structure.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class AltoStepper {
    private static Log log = LogFactory.getLog(AltoStepper.class);

    public Alto step(Alto alto) {
        Alto processedAlto = process(alto);
        if (processedAlto == null) {
            return null;
        }

        List<Alto.Page> newLayout = new ArrayList<Alto.Page>(processedAlto.getLayout().size());
        for (Alto.Page page: processedAlto.getLayout()) {
            Alto.Page processedPage = step(page);
            if (processedPage == null) {
                continue;
            }
            newLayout.add(page);
        }
        processedAlto.getLayout().clear();
        processedAlto.getLayout().addAll(newLayout);
        return processedAlto;
    }


    public Alto.Page step(Alto.Page page) {
        Alto.Page processedPage = process(page);
        if (processedPage == null) {
            return null;
        }

        List<Alto.TextBlock> newBlocks = new ArrayList<Alto.TextBlock>(processedPage.getPrintSpace().size());
        for (Alto.TextBlock block: processedPage.getPrintSpace()) {
            Alto.TextBlock processedBlock = step(block);
            if (processedBlock == null) {
                continue;
            }
            newBlocks.add(processedBlock);
        }
        processedPage.getPrintSpace().clear();
        processedPage.getPrintSpace().addAll(newBlocks);
        return processedPage;
    }

    public Alto.TextBlock step(Alto.TextBlock block) {
        Alto.TextBlock processedBlock = process(block);
        if (processedBlock == null) {
            return null;
        }

        List<Alto.TextLine> newLines = new ArrayList<Alto.TextLine>(processedBlock.getLines().size());
        for (Alto.TextLine line: processedBlock.getLines()) {
            Alto.TextLine processedLine = step(line);
            if (processedLine == null) {
                continue;
            }
            newLines.add(processedLine);
        }
        processedBlock.getLines().clear();
        processedBlock.getLines().addAll(newLines);
        return processedBlock;
    }

    
    public Alto.TextLine step(Alto.TextLine line) {
        Alto.TextLine processedLine = process(line);
        if (processedLine == null) {
            return null;
        }

        List<Alto.TextString> newStrings = new ArrayList<Alto.TextString>(processedLine.getTextStrings().size());
        for (Alto.TextString textString: processedLine.getTextStrings()) {
            Alto.TextString processedString = step(textString);
            if (processedString == null) {
                continue;
            }
            newStrings.add(processedString);
        }
        processedLine.getTextStrings().clear();
        processedLine.getTextStrings().addAll(newStrings);
        return processedLine;
    }

    private Alto.TextString step(Alto.TextString textString) {
        return process(textString);
    }

    /* Override the relevant process-methods */

    public Alto process(Alto alto) {
        log.debug("Returning Alto unmodified");
        return alto;
    }
    public Alto.Page process(Alto.Page page) {
        log.debug("Returning Alto.Page unmodified");
        return page;
    }
    public Alto.TextBlock process(Alto.TextBlock block) {
        log.debug("Returning Alto.TextBlock unmodified");
        return block;
    }
    public Alto.TextLine process(Alto.TextLine line) {
        log.debug("Returning Alto.TextLine unmodified");
        return line;
    }
    public Alto.TextString process(Alto.TextString textString) {
        log.debug("Returning Alto.TextString unmodified");
        return textString;
    }
}
