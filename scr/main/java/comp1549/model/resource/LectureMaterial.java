package comp1549.task2.model.resource;

import comp1549.task2.model.AccessScope;

public class LectureMaterial extends Resource {

    public LectureMaterial(String id, AccessScope scope, String content) {
        super(id, "Lecture Material", scope, content);
    }

    @Override
    public boolean isStaffWritable() {
        return true;
    }
}