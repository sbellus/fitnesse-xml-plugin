package com.github.sbellus.fitnesse.xml;

import fitnesse.plugins.PluginException;
import fitnesse.plugins.PluginFeatureFactoryBase;
import fitnesse.wikitext.parser.SymbolProvider;

/**
 * Register plantuml symbol.
 */
public class XmlPlugin extends PluginFeatureFactoryBase {
    public XmlPlugin() {
    }
    
    public void registerSymbolTypes(SymbolProvider symbolProvider) throws PluginException {
        symbolProvider.add(XmlSymbol.make());
    }
}
