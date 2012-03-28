DOM = Packages.dk.statsbiblioteket.util.xml.DOM;

dom = DOM.stringToDOM(payload.getRecord().getContentAsUTF8());
contents = DOM.selectString(dom, "/root/child2");

payload.getRecord().setContent(contents.getBytes(), false);