// Admin tab: automatic account generation for admitted candidates.
// Feature is planned — shown as "În construcție".
import Icon from '../../components/Icon';

export default function ConturiAdmisi() {
  return (
    <section className="card">
      <div className="card-header">
        <h2 className="card-title">
          <Icon name="group_add" />
          Generare automată conturi admiși
        </h2>
        <span className="badge badge-wip">
          <Icon name="construction" />
          În construcție
        </span>
      </div>
      <div className="card-body">
        <p className="muted">
          Acest modul va permite generarea automată a conturilor pentru candidații
          admiși, pe baza listelor de admitere: creare cont instituțional, atribuirea
          rolului de student, completarea datelor de profil (facultate, specializare,
          grupă, finanțare) și trimiterea credențialelor inițiale.
        </p>

        <div className="mockup-features" style={{ marginTop: 8 }}>
          <div className="mockup-feature">
            <Icon name="upload_file" />
            <span>Import listă admiși (CSV/Excel)</span>
          </div>
          <div className="mockup-feature">
            <Icon name="badge" />
            <span>Generare email + matricol</span>
          </div>
          <div className="mockup-feature">
            <Icon name="manage_accounts" />
            <span>Atribuire rol & specializare</span>
          </div>
          <div className="mockup-feature">
            <Icon name="forward_to_inbox" />
            <span>Trimitere credențiale</span>
          </div>
        </div>

        <div className="mockup-badge" style={{ marginTop: 16 }}>
          <Icon name="construction" />
          Funcționalitate în curs de dezvoltare — disponibilă în curând
        </div>
      </div>
    </section>
  );
}
