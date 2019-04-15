public class CyclomaticComplexity {

    //4
    private String checkForError(int count) {
        if (isReady() && count > 1) {
            return count < 10 ? "Too small" : "Too big";
        }
        return "None";
    }

    /* 1 */
    private boolean isReady() {
        return true;
    }
}