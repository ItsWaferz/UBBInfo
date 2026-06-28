package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.dto.OrarDto;
import ro.ubbcluj.ubbinfo.entity.Orar;
import ro.ubbcluj.ubbinfo.repository.OrarRepository;

import java.util.List;
import java.util.UUID;

/**
 * Timetable. Any authenticated user may read (orar_select_authenticated);
 * only admins may write (orar_admin_write).
 */
@Service
public class OrarService {

    private final OrarRepository orarRepository;
    private final CurrentUserService currentUser;

    public OrarService(OrarRepository orarRepository, CurrentUserService currentUser) {
        this.orarRepository = orarRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<OrarDto> list(String group) {
        List<Orar> rows = (group != null && !group.isBlank())
                ? orarRepository.findForGroupWithRooms(group)
                : orarRepository.findAllWithRooms();
        return rows.stream().map(OrarDto::from).toList();
    }

    /** Distinct group identifiers (semigroup coalesced over group_name). */
    @Transactional(readOnly = true)
    public List<String> groups() {
        return orarRepository.findDistinctGroups().stream().sorted().toList();
    }

    @Transactional
    public OrarDto create(Orar entry) {
        currentUser.requireAdmin();
        entry.setId(null);
        return OrarDto.from(orarRepository.save(entry));
    }

    @Transactional
    public OrarDto update(UUID id, Orar changes) {
        currentUser.requireAdmin();
        Orar e = orarRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orar entry not found: " + id));
        e.setGroupName(changes.getGroupName());
        e.setDayOfWeek(changes.getDayOfWeek());
        e.setStartTime(changes.getStartTime());
        e.setEndTime(changes.getEndTime());
        e.setCourseName(changes.getCourseName());
        e.setType(changes.getType());
        e.setRoom(changes.getRoom());
        e.setProfessor(changes.getProfessor());
        e.setWeekParity(changes.getWeekParity());
        e.setRoomId(changes.getRoomId());
        e.setSemigroup(changes.getSemigroup());
        return OrarDto.from(orarRepository.save(e));
    }

    @Transactional
    public void delete(UUID id) {
        currentUser.requireAdmin();
        if (!orarRepository.existsById(id)) {
            throw new EntityNotFoundException("Orar entry not found: " + id);
        }
        orarRepository.deleteById(id);
    }
}
