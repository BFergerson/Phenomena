public class NestedForStmt {

    public void nestedLoop() throws Exception {
        for (int i = 0; i < 10; i++) {
            for (int z = 0; z < 10; z++) {
                Thread.sleep(100);
            }
        }
    }

}