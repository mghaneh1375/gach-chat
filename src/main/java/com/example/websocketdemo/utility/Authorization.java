package com.example.websocketdemo.utility;

import com.example.websocketdemo.model.Access;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;

import static com.example.websocketdemo.WebsocketDemoApplication.userRepository;

public class Authorization {

    public static boolean isAdmin(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName());
    }

    public static boolean isAdvisor(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.ADVISOR.getName());
    }

    public static boolean isPureStudent(List<String> accesses) {
        return accesses.size() == 1 && accesses.contains(Access.STUDENT.getName());
    }

    public static boolean isStudent(List<String> accesses) {
        return accesses.contains(Access.ADMIN.getName()) ||
                accesses.contains(Access.SUPERADMIN.getName()) ||
                accesses.contains(Access.STUDENT.getName());
    }

    public static boolean hasAccessToThisStudent(ObjectId studentId, ObjectId applicatorId) {

        if(studentId.equals(applicatorId))
            return true;

        Document applicator = userRepository.findById(applicatorId);
        if(applicator == null)
            return false;

        if(isAdmin(applicator.getList("accesses", String.class)))
            return true;

        if(isAdvisor(applicator.getList("accesses", String.class)))
            return Utility.searchInDocumentsKeyValIdx(
                    applicator.getList("students", Document.class), "_id", studentId
            ) > -1;

        return false;
    }
}
