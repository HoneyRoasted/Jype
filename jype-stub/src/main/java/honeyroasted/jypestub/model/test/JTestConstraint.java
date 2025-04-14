package honeyroasted.jypestub.model.test;

import honeyroasted.almonds.Constraint;
import honeyroasted.jype.system.JExpressionInformation;
import honeyroasted.jype.system.JTypeSystem;
import honeyroasted.jype.system.solver.constraints.JTypeConstraints;
import honeyroasted.jype.type.JGenericDeclaration;
import honeyroasted.jype.type.JParameterizedClassType;
import honeyroasted.jype.type.JType;
import honeyroasted.jypestub.model.JStubSerialization;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JTestConstraint {
    Map<String, Class<? extends JTestConstraint>> SUBTYPE_KEYS = Map.of(
            "equal", Equal.class,
            "subtype", Subtype.class,
            "compatible", Compatible.class,
            "expression_compatible", ExpressionCompatible.class,
            "contains", Contains.class,
            "expr_throws", Throws.class,
            "capture", Capture.class,
            "contradiction", Contradiction.class
    );

    Set<Constraint> resolve(JTypeSystem system);

    record Wrapper(JTestConstraint inner) implements JStubSerialization.Wrapper<JTestConstraint> {

        public Set<Constraint> resolve(JTypeSystem system) {
            return this.inner().resolve(system);
        }

        public static JStubSerialization.WrapperSerializer<JTestConstraint, Wrapper> SERIALIZER = new JStubSerialization.WrapperSerializer<>(Wrapper.class, SUBTYPE_KEYS);
        public static JStubSerialization.WrapperDeserializer<JTestConstraint, Wrapper> DESERIALIZER = new JStubSerialization.WrapperDeserializer<>(Wrapper.class, SUBTYPE_KEYS, Wrapper::new);
    }

    record Equal(String declaring, String declaringMethod, List<String> children) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            if (!this.children.isEmpty()) {
                JType previous = JStubSerialization.readType(system, children.getFirst(), containing);
                for (int i = 1; i < this.children.size(); i++) {
                    JType curr = JStubSerialization.readType(system, children.get(i), containing);
                    building.add(new JTypeConstraints.Equal(previous, curr));
                    previous = curr;
                }
            }

            return building;
        }

        public static final JStubSerialization.TypeListDeserializer<Equal> DESERIALIZER = new JStubSerialization.TypeListDeserializer<>(Equal.class, str -> new Equal(null, null, str),
                trip -> new Equal(trip.left(), trip.middle(), trip.right()));
    }

    record Subtype(String declaring, String declaringMethod, List<String> children) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            if (!this.children.isEmpty()) {
                JType previous = JStubSerialization.readType(system, children.getFirst(), containing);
                for (int i = 1; i < this.children.size(); i++) {
                    JType curr = JStubSerialization.readType(system, children.get(i), containing);
                    building.add(new JTypeConstraints.Subtype(previous, curr));
                    previous = curr;
                }
            }

            return building;
        }

        public static final JStubSerialization.TypeListDeserializer<Subtype> DESERIALIZER = new JStubSerialization.TypeListDeserializer<>(Subtype.class, str -> new Subtype(null, null, str),
                trip -> new Subtype(trip.left(), trip.middle(), trip.right()));
    }

    record Compatible(JTypeConstraints.Compatible.Context context, String declaring, String declaringMethod,
                      List<String> children) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            if (!this.children.isEmpty()) {
                JType previous = JStubSerialization.readType(system, children.getFirst(), containing);
                for (int i = 1; i < this.children.size(); i++) {
                    JType curr = JStubSerialization.readType(system, children.get(i), containing);
                    building.add(new JTypeConstraints.Compatible(previous, this.context, curr));
                    previous = curr;
                }
            }

            return building;
        }
    }

    record ExpressionCompatible(JTypeConstraints.Compatible.Context context, JTestExpression.Wrapper expr,
                                String declaring, String declaringMethod, String type) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();
            return Set.of(new JTypeConstraints.ExpressionCompatible(this.expr().resolve(system), this.context, JStubSerialization.readType(system, type, containing)));
        }
    }

    record Contains(String declaring, String declaringMethod, List<String> children) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            if (!this.children.isEmpty()) {
                JType previous = JStubSerialization.readType(system, children.getFirst(), containing);
                for (int i = 1; i < this.children.size(); i++) {
                    JType curr = JStubSerialization.readType(system, children.get(i), containing);
                    building.add(new JTypeConstraints.Contains(previous, curr));
                    previous = curr;
                }
            }

            return building;
        }

        public static final JStubSerialization.TypeListDeserializer<Contains> DESERIALIZER = new JStubSerialization.TypeListDeserializer<>(Contains.class, str -> new Contains(null, null, str),
                trip -> new Contains(trip.left(), trip.middle(), trip.right()));
    }

    record Throws(JTestExpression.Wrapper expr, String declaring, String declaringMethod,
                  String type) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            JExpressionInformation expr = this.expr().resolve(system);
            JType type = JStubSerialization.readType(system, type(), containing);

            if (expr instanceof JExpressionInformation.Lambda jel) {
                return Set.of(new JTypeConstraints.LambdaThrows(jel, type));
            } else if (expr instanceof JExpressionInformation.InvocationReference<?> jir) {
                return Set.of(new JTypeConstraints.InvocationReferenceThrows(jir, type));
            } else if (expr instanceof JExpressionInformation.InstantiationReference jir) {
                return Set.of(new JTypeConstraints.InstantiationReferenceThrows(jir, type));
            }

            return Collections.emptySet();
        }
    }

    record Capture(String declaring, String declaringMethod, List<String> children) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();
            JGenericDeclaration containing = JStubSerialization.declaring(system, this.declaring, this.declaringMethod).left();

            if (!this.children.isEmpty()) {
                JParameterizedClassType previous = JStubSerialization.readType(system, children.getFirst(), containing);
                for (int i = 1; i < this.children.size(); i++) {
                    JParameterizedClassType curr = JStubSerialization.readType(system, children.get(i), containing);
                    building.add(new JTypeConstraints.Capture(previous, curr));
                    previous = curr;
                }
            }

            return building;
        }

        public static final JStubSerialization.TypeListDeserializer<Capture> DESERIALIZER = new JStubSerialization.TypeListDeserializer<>(Capture.class, str -> new Capture(null, null, str),
                trip -> new Capture(trip.left(), trip.middle(), trip.right()));
    }

    record Contradiction(Wrapper left, Wrapper right) implements JTestConstraint {
        @Override
        public Set<Constraint> resolve(JTypeSystem system) {
            Set<Constraint> building = new LinkedHashSet<>();

            Set<Constraint> left = this.left.resolve(system);
            Set<Constraint> right = this.right.resolve(system);

            for (Constraint l : left) {
                for (Constraint r : right) {
                    building.add(new JTypeConstraints.Contradiction(l, r));
                }
            }

            return building;
        }
    }

}
