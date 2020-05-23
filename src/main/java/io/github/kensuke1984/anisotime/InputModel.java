package io.github.kensuke1984.anisotime;

/**
 * Input model for travel time computation
 *
 * @author Kensuke Konishi
 * @version 0.0.3
 */
enum InputModel {
    ANISOTROPIC_PREM("PREM (anisotropic)"), ISOTROPIC_PREM("PREM (isotropic)"), AK135("ak135");

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
