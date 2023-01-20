import java.lang.reflect.Method;

public class SuperBasic {

    public static void main(String[] args) throws Exception {

//        Method myMethod = A.class.getMethod("print1");
        Class<?> classA = Class.forName("SuperBasicA");
        Method myMethod = classA.getMethod("print4");
        SuperBasicA invokeResult = (SuperBasicA) myMethod.invoke(new A(), "ADDB", "ADDC");
        Object invokeResult2 = myMethod.invoke(new A(), "ADDB", "ADDC");
        SuperBasicA invokeResult3 = (SuperBasicA) invokeResult2;

        new SuperBasicA().print1();
        return;
    }

}

class SuperBasicA {

    public void print1() {
        System.out.println("A.print1()");
    }

    public void print2() {
        System.out.println("A.print2()");
    }
    public void print3(String b, String c) {
        System.out.println(b + c);
    }
    public SuperBasicA print4(String b, String c) {
        System.out.println(b + c);
        return new SuperBasicA();
    }

}
