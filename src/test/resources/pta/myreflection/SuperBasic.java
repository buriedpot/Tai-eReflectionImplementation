import java.lang.reflect.Method;

public class SuperBasic {

    public static void main(String[] args) throws Exception {

//        Method myMethod = A.class.getMethod("print1");
        Class<?> classA = Class.forName("A");
        Method myMethod = classA.getMethod("print3");
        Object invokeResult = myMethod.invoke(new A(), "ADDB", "ADDC");
        return;
    }

}

class A {

    public void print1() {
        System.out.println("A.print1()");
    }

    public void print2() {
        System.out.println("A.print2()");
    }
    public void print3(String b, String c) {
        System.out.println(b + c);
    }
}
