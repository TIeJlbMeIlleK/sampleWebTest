package ru.iitgroup.rshbtest;

public class TestInh {


    public static void main(String[] args) {
        TestInh ti = new TestInh();
        ti.doStuff();

    }

    private void doStuff() {
        new B().bMeth()
                .getObject()
                .bMeth()
                .getObject()
                .getObject()
                .getObject()
                .bMeth()
                .bMeth()
                .getObject();
    }

    public abstract class A<T> {

        protected abstract T getSelf();

        T getObject() {
            return getSelf();
        }
    }

    public class B extends A<B> {

        public B bMeth() {
            return this;
        }

        @Override
        protected B getSelf() {
            return this;
        }
    }

}
