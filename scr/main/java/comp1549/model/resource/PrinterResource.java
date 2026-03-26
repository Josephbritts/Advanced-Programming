package comp1549.task2.model.resource;

import comp1549.task2.model.AccessScope;

public class PrinterResource extends Resource {

    public PrinterResource(String id, AccessScope scope, String content) {
        super(id, "Printer", scope, content);
    }

    @Override
    public boolean isStaffWritable() {
        return true;
    }
}