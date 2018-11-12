import com.google.common.collect.Lists;
import java.util.List;

public class InnerMethodIdentifier {
    public static void main(String[] args) {
        List arrayList = Lists.newArrayList();
        MyClass yay = new MyClass();
        arrayList.add(yay);
    }
}