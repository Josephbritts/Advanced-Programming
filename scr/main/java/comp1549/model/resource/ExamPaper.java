package comp1549.task2.model.resource;

import comp1549.task2.model.AccessScope;

public class ExamPaper extends Resource {

    public ExamPaper(String id, AccessScope scope, String content) {
        super(id, "Exam Paper", scope, content);
    }
}