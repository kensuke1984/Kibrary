package io.github.kensuke1984.anisotime;


class ANISOtimeTest {
    public static void main(String[] args) throws InterruptedException {
//        downloadANISOtime();
        ANISOtime.main("-mod prem -ph PKJKP -rs 180,200,1 -o /tmp/rctmp".split(" "));
//        System.out.println(Complex.class.getProtectionDomain().getCodeSource().getLocation());
    }

}
