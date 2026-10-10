package icbm.classic.api.actions.data;

/**
 * Wrapper to provide fields
 *
 */
public interface IActionFieldReceiver {

    /**
     * Called to set a value
     *
     * @param key to set
     * @param value to set
     */
    default <VALUE> void setValue(ActionField<VALUE> key, VALUE value) {

    }

    default <SELF extends IActionFieldReceiver> SELF applyFields(IActionFieldProvider fieldAccessor) {
        if (fieldAccessor != null && !fieldAccessor.getFields().isEmpty()) {
            for (ActionField<?> field : fieldAccessor.getFields()) {
                applyField(field, fieldAccessor);
            }
        }
        return (SELF) this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyField(ActionField field, IActionFieldProvider fieldAccessor) {
        this.setValue(field, fieldAccessor.getValue(field));
    }
}
