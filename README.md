Copy from https://github.com/Viola-Siemens/Initial-House and https://github.com/Serilum/Starter-Structure

because Im too lazy to add config, so you can change the spawn strcuture dimension by change this:

public void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			IHSavedData worldData = world.getDataStorage().computeIfAbsent(IHSavedData::new, IHSavedData::new, IHSavedData.SAVED_DATA_NAME);
			IHSavedData.setInstance(worldData);
		}
	}
