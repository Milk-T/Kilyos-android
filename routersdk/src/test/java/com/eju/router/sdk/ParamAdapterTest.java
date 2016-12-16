package com.eju.router.sdk;

import android.net.Uri;
import android.os.Bundle;

import com.eju.router.sdk.exception.EjuParamException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.assertj.core.api.Assertions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ParamAdapterUnitTest
 *
 * @author tangqianwei
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, manifest = Config.NONE)
public class ParamAdapterTest extends BaseTest {

    private final class TestParam implements Serializable {
        String s1;
        String s2;
        List<String> l1;
    }

    private ParamAdapter mAdapter;

    @Override
    public void setUp() {
        super.setUp();

        mAdapter = new ParamAdapter();
    }

    @Test
    public void testGetStringParamConverter() {
        try {
            ParamAdapter.getParamConverter(String.class);
        } catch (Exception e) {
            Assertions.fail("cannot get String converter");
        }
    }

    @Test
    public void testGetListParamConverter() {
        try {
            ParamAdapter.getParamConverter(List.class);
        } catch (Exception e) {
            Assertions.fail("cannot get List converter");
        }
    }

    @Test
    public void testAddCustomizeParamConverter() {
        ParamAdapter.addParamConverter(TestParam.class, new ParamConverter<TestParam>() {
            @Override
            public String toUrl(TestParam testParam) {
                StringBuilder builder = new StringBuilder();
                builder.append("{");

                builder.append("\"s1\":");
                builder.append('"');
                builder.append(testParam.s1);
                builder.append('"');

                builder.append(",");

                builder.append("\"s2\":");
                builder.append('"');
                builder.append(testParam.s2);
                builder.append('"');

                if (null != testParam.l1 && 0 < testParam.l1.size()) {
                    builder.append(",");

                    builder.append("\"l1\":");
                    builder.append('[');
                    for (String s : testParam.l1) {
                        builder.append('"');
                        builder.append(s);
                        builder.append('"');
                        builder.append(',');
                    }
                    builder.append(']');
                }

                builder.append('}');

                return builder.toString();
            }

            @Override
            public TestParam fromUrl(String query) throws EjuParamException {
                return null;
            }
        });

        try {
            ParamAdapter.getParamConverter(TestParam.class);
        } catch (Exception e) {
            Assertions.fail("add ParamConverter fail");
        }
    }

    @Test
    public void testAddDefaultParamConverter() {
        ParamAdapter.addParamConverter(TestParam.class);

        try {
            ParamAdapter.getParamConverter(TestParam.class);
        } catch (Exception e) {
            Assertions.fail("add default ParamConverter fail");
        }
    }

    @Test
    public void testPrimitiveToUrl() {
        Bundle bundle = new Bundle();
        bundle.putString("string", "aaa");
        bundle.putInt("int", 1024);
        bundle.putBoolean("boolean", false);
        bundle.putChar("char", 'a');
        bundle.putByte("byte", (byte)0xf0);
        bundle.putShort("short", (short)0x0f);
        bundle.putFloat("float", 123.45f);
        bundle.putDouble("double", 1.2345123456458764);
        bundle.putLong("long", 0xffff00000000ffffL);

        mAdapter.setParam(bundle);
        try {
            String url = mAdapter.toURL();

            Assertions.assertThat(url).isEqualTo(
                    "boolean=false&string=aaa&double=1.2345123456458764" +
                            "&byte=-16&char=a&short=15&float=123.45&int=1024" +
                            "&long=-281474976645121");

        } catch (EjuParamException e) {
            Assertions.fail("primitive to url fail");
        }
    }

    @Test
    public void testSerializableToUrl() {
        TestParam param = new TestParam();
        param.s1 = "first";
        param.s2 = "second";
        param.l1 = new ArrayList<>();
        param.l1.add("item1");
        param.l1.add("item2");

        Bundle bundle = new Bundle();
        bundle.putSerializable("param1", param);

        mAdapter.setParam(bundle);
        try {
            String url = mAdapter.toURL();
            Assertions.assertThat(url).isEqualTo(
                    "param1=%7B%22s1%22%3A%22first%22%2C%22s2%22%3A%22second%22%2C%22l1%22%3A%5B%22item1%22%2C%22item2%22%5D%7D");
        } catch (EjuParamException e) {
            Assertions.fail("serializable to url fail");
        }
    }

    @Test
    public void testToBundle() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "aaa");
        map.put("int", 1024);
        map.put("boolean", false);
        map.put("char", 'a');
        map.put("byte", (byte)0xf0);
        map.put("short", (short)0x0f);
        map.put("float", 123.45f);
        map.put("double", 1.2345123456458765138732165873621354876f);
        map.put("long", 0xffff00000000ffffL);

        mAdapter.setParam(map);
        Bundle bundle = mAdapter.toBundle();
        Assertions.assertThat(bundle.get("string")).isNotNull().isEqualTo("aaa");
        Assertions.assertThat(bundle.get("int")).isNotNull().isEqualTo(1024);
        Assertions.assertThat(bundle.get("boolean")).isNotNull().isEqualTo(false);
        Assertions.assertThat(bundle.get("char")).isNotNull().isEqualTo('a');
        Assertions.assertThat(bundle.get("byte")).isNotNull().isEqualTo((byte)0xf0);
        Assertions.assertThat(bundle.get("short")).isNotNull().isEqualTo((short)0x0f);
        Assertions.assertThat(bundle.get("float")).isNotNull().isEqualTo(123.45f);
        Assertions.assertThat(bundle.get("double"))
                .isNotNull().isEqualTo(1.2345123456458765138732165873621354876f);
        Assertions.assertThat(bundle.get("long")).isNotNull().isEqualTo(0xffff00000000ffffL);
    }

    @Test
    public void testListStringParam() {
        ArrayList<String> stringList = new ArrayList<>();
        stringList.add("abc");
        stringList.add("123");
        stringList.add("AABBCC");

        Bundle bundle = new Bundle();
        bundle.putStringArrayList("param", stringList);

        mAdapter.setParam(bundle);
        try {
            String url = mAdapter.toURL();
            Assertions.assertThat(url).isEqualTo(
                    "param[]=abc&param[]=123&param[]=AABBCC");
        } catch (EjuParamException e) {
            Assertions.fail("list to url fail");
        }
    }

    @Test
    public void testListComplexParam() {
        ArrayList<Serializable> list = new ArrayList<>();
        list.add(newTestParam("a", "1", null));
        list.add(newTestParam("b", "2", null));
        list.add(newTestParam("c", "3", null));

        Map<String, Object> bundle = new HashMap<>();
        bundle.put("param", list);

        mAdapter.setParam(bundle);
        try {
            String url = mAdapter.toURL();
            Assertions.assertThat(url).isEqualTo(String.format(
                    "param[]=%s&param[]=%s&param[]=%s",
                    Uri.encode("{\"s1\":\"a\",\"s2\":\"1\"}"),
                    Uri.encode("{\"s1\":\"b\",\"s2\":\"2\"}"),
                    Uri.encode("{\"s1\":\"c\",\"s2\":\"3\"}")));
        } catch (EjuParamException e) {
            Assertions.fail("complex to url fail");
        }
    }

    @Test
    public void testPrimitiveFromUrl() {
        Bundle bundle = mAdapter.fromUrl("string=aaa&int=1024&boolean=false" +
                "&char=a&byte=-16&short=15&float=123.45" +
                "&double=1.2345123456458764" +
                "&long=-281474976645121");
        Assertions.assertThat(bundle.size()).isNotNull().isGreaterThan(0);
    }

    @Test
    public void testWrapperObjectFromUrl() {
        ParamAdapter.addParamConverter(TestParam.class);
        Bundle bundle = mAdapter.fromUrl(
                "param1=%7B%22s1%22%3A%22first%22%2C%22s2%22%3A%22second%22%2C%22l1%22%3A%5B" +
                        "%22item1%22%2C%22item2%22%5D%7D");
                //"param1={\"s1\":\"first\",\"s2\":\"second\",\"l1\":[\"item1\",\"item2\"]}");
        Assertions.assertThat(bundle.size()).isNotNull().isGreaterThan(0);
    }

    private TestParam newTestParam(String s1, String s2, List<String> list) {
        TestParam param = new TestParam();
        param.s1 = s1;
        param.s2 = s2;
        param.l1 = list;
        return param;
    }
}
