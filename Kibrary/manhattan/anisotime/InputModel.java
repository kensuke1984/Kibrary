package anisotime;

/**
 * @author kensuke
 * @since 2014/07/29
 * @version 0.0.1
 * 
 *          Input model for travel time computation
 * 
 */
enum InputModel {
	ANISOTROPIC_PREM("PREM (anisotropic)"), ISOTROPIC_PREM("PREM (isotropic)"), AK135("ak135"), POLYNOMIAL(
			"polynomial"), NAMED_DISCONTINUITY("named discontinuity");

	final String title;

	private InputModel(String string) {
		title = string;
	}
	
	/**
	 * @param title
	 * @return InputModel which has the title
	 */
	static InputModel titleOf(String title) {
		for (InputModel model : values())
			if (model.title.equals(title))
				return model;
		return null;

	}

}
