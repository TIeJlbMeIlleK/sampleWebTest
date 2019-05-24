package ru.iitgroup.rshbtest;

public class TestInh {


    public static void main(String[] args) {

        TestInh ti = new TestInh();
        ti.doStuff();

    }

    private void doStuff() {
        B b = new B();
        b
                .bMeth()
                .getObject()
                .bMeth()
                .getObject();



    }

    public class A {

        A getObject() {
            return  this;
        }
    }

    public class B extends A {

        public B bMeth() {
            return this;
        }

        @Override
        B getObject() {
            return (B) super.getObject();
        }
    }

}
