package ro.ubbcluj.ubbinfo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** public.useful_links — dashboard quick links, with per-language title/url overrides. */
@Entity
@Table(name = "useful_links")
public class UsefulLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "title")
    private String title;

    @Column(name = "title_en")
    private String titleEn;

    @Column(name = "title_hu")
    private String titleHu;

    @Column(name = "title_de")
    private String titleDe;

    @Column(name = "url")
    private String url;

    @Column(name = "url_en")
    private String urlEn;

    @Column(name = "url_hu")
    private String urlHu;

    @Column(name = "url_de")
    private String urlDe;

    @Column(name = "icon")
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getTitleHu() { return titleHu; }
    public void setTitleHu(String titleHu) { this.titleHu = titleHu; }

    public String getTitleDe() { return titleDe; }
    public void setTitleDe(String titleDe) { this.titleDe = titleDe; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUrlEn() { return urlEn; }
    public void setUrlEn(String urlEn) { this.urlEn = urlEn; }

    public String getUrlHu() { return urlHu; }
    public void setUrlHu(String urlHu) { this.urlHu = urlHu; }

    public String getUrlDe() { return urlDe; }
    public void setUrlDe(String urlDe) { this.urlDe = urlDe; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
