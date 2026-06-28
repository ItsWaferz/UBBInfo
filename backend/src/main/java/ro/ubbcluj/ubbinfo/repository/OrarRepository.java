package ro.ubbcluj.ubbinfo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ubbcluj.ubbinfo.entity.Orar;

import java.util.List;
import java.util.UUID;

public interface OrarRepository extends JpaRepository<Orar, UUID> {

    /**
     * Timetable for a semigroup, with rooms+buildings fetched. Matches the
     * frontend filter: rows whose semigroup = :group, OR (semigroup is null AND
     * group_name = :group).
     */
    @Query("""
            select o from Orar o
            left join fetch o.roomRef r
            left join fetch r.building b
            where o.semigroup = :group
               or (o.semigroup is null and o.groupName = :group)
            order by o.dayOfWeek, o.startTime
            """)
    List<Orar> findForGroupWithRooms(@Param("group") String group);

    @Query("""
            select o from Orar o
            left join fetch o.roomRef r
            left join fetch r.building b
            order by o.dayOfWeek, o.startTime
            """)
    List<Orar> findAllWithRooms();

    /** Distinct group identifiers, coalescing semigroup over group_name (frontend's logic). */
    @Query("select distinct coalesce(o.semigroup, o.groupName) from Orar o "
            + "where coalesce(o.semigroup, o.groupName) is not null")
    List<String> findDistinctGroups();
}
