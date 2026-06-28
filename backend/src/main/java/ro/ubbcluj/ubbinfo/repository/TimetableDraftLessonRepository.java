package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ubbcluj.ubbinfo.entity.TimetableDraftLesson;

import java.util.List;
import java.util.UUID;

public interface TimetableDraftLessonRepository extends JpaRepository<TimetableDraftLesson, UUID> {

    List<TimetableDraftLesson> findByDraftIdOrderByDayOfWeekAscStartTimeAsc(UUID draftId);

    void deleteByDraftId(UUID draftId);
}
