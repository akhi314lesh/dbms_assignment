import { useState } from 'react'

const devices = [
  {
    name: 'Smart Lights',
    description: 'Adaptive lighting that responds to your routines.',
    icon: '☼',
  },
  {
    name: 'Thermostats',
    description: 'Intelligent climate control for effortless comfort.',
    icon: '◉',
  },
  {
    name: 'Temperature Sensors',
    description: 'Real-time temperature monitoring throughout your home.',
    icon: '♨',
  },
  {
    name: 'Motion Sensors',
    description: 'Quietly monitor movement and activity in every room.',
    icon: '⌁',
  },
  {
    name: 'Cameras',
    description: 'Keep an eye on what matters, wherever you are.',
    icon: '◌',
  },
]

function Landing({ onHome }) {
  const [activeDevice, setActiveDevice] = useState(null)

  return (
    <main className="landing-page">

      {/* Navigation */}
      <nav className="landing-nav">
        <div className="brand">
          <img src="/logo.svg" alt="Smart Home" />
          <span>Smart Home</span>
        </div>

        <div className="nav-links">
          <a href="#devices">Devices</a>
          <a href="#about">About</a>

          <button
            className="nav-button"
            onClick={() => window.location.reload()}
          >
            Sign Out
          </button>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="hero-content">

          <p className="eyebrow">
            INTELLIGENT LIVING · CONNECTED HOME
          </p>

          <h1>
            Smarter and
            <br />
            <span>safer living.</span>
          </h1>

          <p className="hero-description">
            Intelligent technology designed around the way you live.
            Monitor, manage, and control your home with simplicity.
          </p>

          <div className="hero-actions">

            <a
              href="#devices"
              className="primary-button"
            >
              Explore your home →
            </a>

            {/* Home Query Button */}
            <button
              className="home-query-button"
              onClick={onHome}
            >
              <span>✦</span>
              <span>Open Home Query</span>
              <span>→</span>
            </button>

            <span className="hero-note">
              Sanctuary Active
            </span>

          </div>

        </div>

        <div className="hero-decoration">
          <div className="decoration-circle circle-one"></div>
          <div className="decoration-circle circle-two"></div>

          <div className="home-card">
            <img src="/logo.svg" alt="" />
            <span>48B</span>
          </div>
        </div>
      </section>

      {/* Devices */}
      <section
        className="devices-section"
        id="devices"
      >

        <div className="section-heading">

          <div>
            <p className="eyebrow">
              CONNECTED LIVING
            </p>

            <h2>
              Your home,
              <br />
              <span>intelligently connected.</span>
            </h2>
          </div>

          <p className="section-description">
            Every device works together to create a home
            that understands your needs.
          </p>

        </div>

        <div className="device-grid">

          {devices.map((device, index) => (
            <div
              className={`device-card ${
                activeDevice === index
                  ? 'device-card-active'
                  : ''
              }`}
              key={device.name}
              onMouseEnter={() => setActiveDevice(index)}
              onMouseLeave={() => setActiveDevice(null)}
            >

              <div className="device-top">

                <span className="device-number">
                  0{index + 1}
                </span>

                <span className="device-icon">
                  {device.icon}
                </span>

              </div>

              <div className="device-info">

                <h3>
                  {device.name}
                </h3>

                <p>
                  {device.description}
                </p>

                <span className="device-arrow">
                  Explore →
                </span>

              </div>

            </div>
          ))}

        </div>

      </section>

      {/* About */}
      <section
        className="about-section"
        id="about"
      >

        <div className="about-line"></div>

        <p className="eyebrow">
          THE SMART HOME PHILOSOPHY
        </p>

        <h2>
          Technology should feel
          <br />
          <span>effortless.</span>
        </h2>

        <p className="about-text">
          From adaptive lighting to intelligent security,
          Smart Home brings every part of your home together
          in one calm, considered experience.
        </p>

      </section>

      {/* Footer */}
      <footer className="landing-footer">

        <div className="brand">
          <img src="/logo.svg" alt="Smart Home" />
          <span>Smart Home</span>
        </div>

        <p>
          Smarter and safer living.
        </p>

        <span>
          © 2026 Smart Home
        </span>

      </footer>

    </main>
  )
}

export default Landing