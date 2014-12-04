package org.backmeup.index.core.elasticsearch.tokenreader;

import java.util.HashMap;
import java.util.Map;

public class MapTokenResolver implements ITokenResolver {

    private Map<String, String> tokenMap = new HashMap<>();

    public MapTokenResolver(Map<String, String> tokenMap) {
        this.tokenMap = tokenMap;
    }

    @Override
    public String resolveToken(String tokenName) {
        return this.tokenMap.get(tokenName);
    }

}