import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.*;

/**
 * Usage (needs asm + asm-tree 9.x on the class path):
 *   javac -cp asm.jar;asm-tree.jar -d out MixinTargetCheck.java
 *   java -cp out;asm.jar;asm-tree.jar MixinTargetCheck <named minecraft jar> <dir with compiled mixin classes>
 * The mixin classes can be compiled in isolation against stubs of IFramedBlock & co. (javac -proc:none).
 *
 * Offline sanity check of mixin targets: for every handler annotation of the compiled mixin classes it checks that the
 * target method exists in the (named) 1.20.1 Minecraft jar, that INVOKE targets exist in it, and that ordinals are in range.
 */
public class MixinTargetCheck
{
    static JarFile mc;
    static Map<String, ClassNode> cache = new HashMap<>();
    static int problems = 0;
    static int checked = 0;

    static ClassNode load(String internal) throws Exception
    {
        if (cache.containsKey(internal)) return cache.get(internal);
        var entry = mc.getEntry(internal + ".class");
        ClassNode node = null;
        if (entry != null)
        {
            try (InputStream in = mc.getInputStream(entry))
            {
                node = new ClassNode();
                new ClassReader(in).accept(node, 0);
            }
        }
        cache.put(internal, node);
        return node;
    }

    static Object get(AnnotationNode a, String key)
    {
        if (a.values == null) return null;
        for (int i = 0; i < a.values.size(); i += 2)
        {
            if (a.values.get(i).equals(key)) return a.values.get(i + 1);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o)
    {
        if (o == null) return List.of();
        if (o instanceof List<?> l) return (List<Object>) l;
        return List.of(o);
    }

    static void problem(String msg)
    {
        problems++;
        System.out.println("PROBLEM: " + msg);
    }

    public static void main(String[] args) throws Exception
    {
        mc = new JarFile(args[0]);
        List<Path> files;
        try (Stream<Path> s = Files.walk(Paths.get(args[1])))
        {
            files = s.filter(p -> p.toString().endsWith(".class")).sorted().collect(Collectors.toList());
        }
        for (Path p : files)
        {
            ClassNode mixin = new ClassNode();
            new ClassReader(Files.readAllBytes(p)).accept(mixin, 0);
            AnnotationNode mixinAnn = null;
            for (AnnotationNode a : all(mixin.visibleAnnotations, mixin.invisibleAnnotations))
            {
                if (a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) mixinAnn = a;
            }
            if (mixinAnn == null) continue;
            List<String> targets = new ArrayList<>();
            for (Object o : list(get(mixinAnn, "value"))) targets.add(((Type) o).getInternalName());
            for (Object o : list(get(mixinAnn, "targets"))) targets.add(((String) o).replace('.', '/'));
            String mname = mixin.name.substring(mixin.name.lastIndexOf('/') + 1);

            // shadows
            for (FieldNode f : mixin.fields)
            {
                if (hasAnn(all(f.visibleAnnotations, f.invisibleAnnotations), "Lorg/spongepowered/asm/mixin/Shadow;"))
                {
                    for (String t : targets)
                    {
                        if (findField(t, f.name, f.desc) == null) problem(mname + ": @Shadow field " + f.name + " " + f.desc + " not found in " + t);
                        else { checked++; System.out.println("ok   " + mname + " @Shadow field " + f.name + " in " + t); }
                    }
                }
            }
            for (MethodNode m : mixin.methods)
            {
                if (hasAnn(all(m.visibleAnnotations, m.invisibleAnnotations), "Lorg/spongepowered/asm/mixin/Shadow;"))
                {
                    for (String t : targets)
                    {
                        if (findMethod(t, m.name, m.desc) == null) problem(mname + ": @Shadow method " + m.name + m.desc + " not found in " + t);
                        else { checked++; System.out.println("ok   " + mname + " @Shadow method " + m.name + " in " + t); }
                    }
                }
                for (AnnotationNode a : all(m.visibleAnnotations, m.invisibleAnnotations))
                {
                    String kind = a.desc.substring(a.desc.lastIndexOf('/') + 1, a.desc.length() - 1);
                    if (kind.equals("Invoker") || kind.equals("Accessor"))
                    {
                        String name = (String) get(a, "value");
                        for (String t : targets)
                        {
                            boolean found = false;
                            for (var mm : allMethods(t)) if (mm.name.equals(name) && Type.getArgumentTypes(mm.desc).length == Type.getArgumentTypes(m.desc).length && mm.desc.equals(m.desc)) found = true;
                            if (!found) problem(mname + "#" + m.name + ": @" + kind + " " + name + m.desc + " not found in " + t);
                            else { checked++; System.out.println("ok   " + mname + "#" + m.name + " @" + kind + " " + name + m.desc + " in " + t); }
                        }
                        continue;
                    }
                    if (!(kind.equals("WrapOperation") || kind.equals("ModifyExpressionValue") || kind.equals("ModifyReturnValue") || kind.equals("Inject") || kind.equals("Redirect") || kind.equals("ModifyArg") || kind.equals("ModifyVariable"))) continue;
                    List<Object> methods = list(get(a, "method"));
                    List<Object> ats = list(get(a, "at"));
                    for (String t : targets)
                    {
                        for (Object mo : methods)
                        {
                            String spec = (String) mo;
                            List<MethodNode> matched = resolve(t, spec);
                            if (matched.isEmpty())
                            {
                                problem(mname + "#" + m.name + ": target method '" + spec + "' not found in " + t);
                                continue;
                            }
                            if (matched.size() > 1 && !spec.contains("(")) System.out.println("note " + mname + "#" + m.name + ": '" + spec + "' matches " + matched.size() + " overloads in " + t);
                            for (Object ao : ats)
                            {
                                AnnotationNode at = (AnnotationNode) ao;
                                String value = (String) get(at, "value");
                                String target = (String) get(at, "target");
                                Object ordO = get(at, "ordinal");
                                int ordinal = ordO == null ? -1 : (Integer) ordO;
                                if (!"INVOKE".equals(value) || target == null)
                                {
                                    checked++;
                                    System.out.println("ok   " + mname + "#" + m.name + " " + kind + " " + t.substring(t.lastIndexOf('/') + 1) + "#" + spec + " @" + value);
                                    continue;
                                }
                                int sep = target.indexOf(';');
                                String owner = target.substring(1, sep);
                                String rest = target.substring(sep + 1);
                                int paren = rest.indexOf('(');
                                String name = rest.substring(0, paren);
                                String desc = rest.substring(paren);
                                for (MethodNode tm : matched)
                                {
                                    int count = 0;
                                    for (AbstractInsnNode insn : tm.instructions)
                                    {
                                        if (insn instanceof MethodInsnNode mi && mi.owner.equals(owner) && mi.name.equals(name) && mi.desc.equals(desc)) count++;
                                    }
                                    if (count == 0) problem(mname + "#" + m.name + ": INVOKE " + target + " not found in " + t + "#" + tm.name + tm.desc);
                                    else if (ordinal >= count) problem(mname + "#" + m.name + ": ordinal " + ordinal + " >= " + count + " for " + target + " in " + tm.name);
                                    else
                                    {
                                        checked++;
                                        System.out.println("ok   " + mname + "#" + m.name + " " + kind + " " + t.substring(t.lastIndexOf('/') + 1) + "#" + tm.name + " INVOKE " + owner.substring(owner.lastIndexOf('/') + 1) + "." + name + " sites=" + count + " ordinal=" + ordinal);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("checked " + checked + ", problems " + problems);
    }

    static List<AnnotationNode> all(List<AnnotationNode> a, List<AnnotationNode> b)
    {
        List<AnnotationNode> r = new ArrayList<>();
        if (a != null) r.addAll(a);
        if (b != null) r.addAll(b);
        return r;
    }

    static boolean hasAnn(List<AnnotationNode> l, String desc)
    {
        if (l == null) return false;
        for (AnnotationNode a : l) if (a.desc.equals(desc)) return true;
        return false;
    }

    static List<MethodNode> allMethods(String cls) throws Exception
    {
        List<MethodNode> r = new ArrayList<>();
        ClassNode c = load(cls);
        while (c != null)
        {
            r.addAll(c.methods);
            c = c.superName == null ? null : load(c.superName);
        }
        return r;
    }

    static MethodNode findMethod(String cls, String name, String desc) throws Exception
    {
        for (MethodNode m : allMethods(cls)) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        return null;
    }

    static FieldNode findField(String cls, String name, String desc) throws Exception
    {
        ClassNode c = load(cls);
        while (c != null)
        {
            for (FieldNode f : c.fields) if (f.name.equals(name) && f.desc.equals(desc)) return f;
            c = c.superName == null ? null : load(c.superName);
        }
        return null;
    }

    static List<MethodNode> resolve(String cls, String spec) throws Exception
    {
        String name = spec;
        String desc = null;
        if (spec.startsWith("L") && spec.indexOf(';') > 0)
        {
            name = spec.substring(spec.indexOf(';') + 1);
        }
        int paren = name.indexOf('(');
        if (paren >= 0)
        {
            desc = name.substring(paren);
            name = name.substring(0, paren);
        }
        ClassNode c = load(cls);
        List<MethodNode> r = new ArrayList<>();
        if (c == null) return r;
        for (MethodNode m : c.methods)
        {
            if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) r.add(m);
        }
        return r;
    }
}
