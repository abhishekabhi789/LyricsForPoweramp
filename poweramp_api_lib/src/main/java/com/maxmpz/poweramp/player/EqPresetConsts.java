package com.maxmpz.poweramp.player;

/**
 * @see TableDefs.EqPresets#TYPE
 * @noinspection InterfaceNeverImplemented
 */
public interface EqPresetConsts {
	/** Preset type not set */
    int TYPE_UNKNOWN = -1;

	/** User created or default user preset */
    int TYPE_USER = 0;

	/** Built-in preset */
    int TYPE_BUILT_IN = 10;

	/** AutoEq preset */
    int TYPE_AUTO_EQ = 20;
}
