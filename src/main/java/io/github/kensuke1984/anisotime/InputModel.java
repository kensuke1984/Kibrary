package io.github.kensuke1984.anisotime;

/**
 * Input model for travel time computation
 *
 * @author Kensuke Konishi
 * @version 0.0.2.1
 */
enum InputModel {
    ANISOTROPIC_PREM("PREM (anisotropic)"), ISOTROPIC_PREM("PREM (isotropic)"), AK135("ak135"),
    POLYNOMIAL("polynomial"), NAMED_DISCONTINUITY("named discontinuity");

    /**
     * name of model
     */
    final String name;

    InputModel(String string) {
        name = string;
    }

    /**
     * @param title to look for
     * @return InputModel which has the title
     */
    static InputModel titleOf(String title) {
        for (InputModel model : values())
            if (model.name.equals(title)) return model;
        throw new IllegalArgumentException("There is no " + title);
    }

}
