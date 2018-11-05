public class NeverExecutedCode {
    public void methodStatic () {
        int i = 10;
        if (i == 11) {
            System.out.println("Never executed output");
        } else {
            System.out.println("Always executed output");
        }

        while (i < 4) {
            System.out.println("Also never executed output");
        }

        while (i > 4) {
            System.out.println("Infinite loop here");
        }
    }

    public void methodDynamic (int i) {
        if (i == 11) {
            System.out.println("Never executed output");
        } else {
            System.out.println("Always executed output");
        }

        while (i < 4) {
            System.out.println("Also never executed output");
        }

        while (i > 4) {
            System.out.println("Infinite loop here");
        }
    }
}