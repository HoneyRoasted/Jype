package honeyroasted.jype.system.solver.inference.model.adapter;

import honeyroasted.jype.type.TypeDeclaration;

import java.util.List;

public class MultiExpressionAdapter implements ExpressionAdapter {
    private List<ExpressionAdapter> adapters;

    public MultiExpressionAdapter(List<ExpressionAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    @Override
    public boolean isFunctionalInterface(TypeDeclaration type) {
        for (ExpressionAdapter adapter : this.adapters) {
            if (adapter.isFunctionalInterface(type)) {
                return true;
            }
        }
        return false;
    }
}
