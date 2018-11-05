class Factorial {

    public static int main(String args[]) {
        int fact;
        int n;
        n = 4;
        fact = 1;

        //some useless code to test control flow
        if (false) {
            int x = 5;
            System.out.println("Code that does nothing");
        }

        while (n != 0) {
            fact = fact * n;
            n = n - 1;
        }
        return fact;
    }

}