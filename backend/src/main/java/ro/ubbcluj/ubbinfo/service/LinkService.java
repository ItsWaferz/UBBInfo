package ro.ubbcluj.ubbinfo.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ubbcluj.ubbinfo.entity.UsefulLink;
import ro.ubbcluj.ubbinfo.repository.UsefulLinkRepository;

import java.util.List;
import java.util.UUID;

/**
 * Useful links. Read is world-readable (the dashboard shows active links);
 * create/update/delete are admin-only (admin_*_links RLS).
 */
@Service
public class LinkService {

    private final UsefulLinkRepository linkRepository;
    private final CurrentUserService currentUser;

    public LinkService(UsefulLinkRepository linkRepository, CurrentUserService currentUser) {
        this.linkRepository = linkRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UsefulLink> list(boolean activeOnly) {
        return activeOnly
                ? linkRepository.findByIsActiveTrueOrderBySortOrderAsc()
                : linkRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public UsefulLink create(UsefulLink link) {
        currentUser.requireAdmin();
        link.setId(null);
        return linkRepository.save(link);
    }

    @Transactional
    public UsefulLink update(UUID id, UsefulLink changes) {
        currentUser.requireAdmin();
        UsefulLink e = linkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Link not found: " + id));
        e.setTitle(changes.getTitle());
        e.setTitleEn(changes.getTitleEn());
        e.setTitleHu(changes.getTitleHu());
        e.setTitleDe(changes.getTitleDe());
        e.setUrl(changes.getUrl());
        e.setUrlEn(changes.getUrlEn());
        e.setUrlHu(changes.getUrlHu());
        e.setUrlDe(changes.getUrlDe());
        e.setIcon(changes.getIcon());
        e.setSortOrder(changes.getSortOrder());
        if (changes.getIsActive() != null) {
            e.setIsActive(changes.getIsActive());
        }
        return linkRepository.save(e);
    }

    @Transactional
    public void delete(UUID id) {
        currentUser.requireAdmin();
        if (!linkRepository.existsById(id)) {
            throw new EntityNotFoundException("Link not found: " + id);
        }
        linkRepository.deleteById(id);
    }
}
