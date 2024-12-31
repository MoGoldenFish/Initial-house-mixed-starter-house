package com.hexagram2021.initial_house.server.util;

import java.util.ServiceLoader;

public class Services {

    public static final ModLoaderHelper MODLOADER = load(ModLoaderHelper.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("[MoFish] Failed to load service for " + clazz.getName() + "."));
    }
}
