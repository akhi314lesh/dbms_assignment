import { useState } from 'react'
import penthouse from "../assets/screen.png";

function Home() {
  const [query, setQuery] = useState('')
  const [visualised, setVisualised] = useState(false)
  const [loading, setLoading] = useState(false)

  const presets = [
    {
      name: 'Evening Reading',
      text: 'Prepare living room for evening reading with warm dimming',
    },
    {
      name: 'Circadian Climate',
      text: 'Optimize circadian lighting, balance airflow, and set 71°F',
    },
    {
      name: 'Secure Perimeter',
      text: 'Engage perimeter security locks and lower bronze acoustic louvers',
    },
  ]

  const handlePreset = (text) => {
    setQuery(text)
    setVisualised(false)
  }

  const handleVisualise = () => {
    const finalQuery =
      query.trim() ||
      'Prepare living room for evening reading and soft warmth'

    setLoading(true)
    setVisualised(false)

    setTimeout(() => {
      setLoading(false)
      setVisualised(true)
    }, 650)
  }

  const getResult = () => {
    const lowerQuery = query.toLowerCase()

    if (
      lowerQuery.includes('circadian') ||
      lowerQuery.includes('climate')
    ) {
      return {
        title: 'Circadian Optimization Engaged',
        description:
          'Dynamic spectral compensation calibrated for sunset. HVAC balanced to 71.0°F with continuous laminar airflow across floor-level returns.',
      }
    }

    if (
      lowerQuery.includes('secure') ||
      lowerQuery.includes('lock')
    ) {
      return {
        title: 'Sanctuary Perimeter Secured',
        description:
          'Magnetic mortise deadbolts double-locked. Bronze acoustic motorized shutters lowered to privacy detent. Exterior optical sensors placed in high-vigilance telemetry.',
      }
    }

    return {
      title: 'Penthouse Ambience Shifted',
      description: `Applied intention: "${
        query ||
        'Prepare living room for evening reading and soft warmth'
      }". 2,700K architectural warm dimming engaged across salon cove tracks, linen draperies positioned, thermal zones quieted.`,
    }
  }

  const result = getResult()

  return (
    <div className="home-page">

      {/* Background Image */}
      <div
        className="home-background"
        style={{ backgroundImage: `url(${penthouse})` }}
      />

      {/* Light overlay */}
      <div className="home-background-overlay" />

      {/* Page Content */}
      <div className="home-content">

        {/* Header */}
        <header className="home-header">

          <div className="home-badge">
            <span className="home-badge-dot"></span>
            SANCTUARY QUERY ENGINE • V4.2 MESH
          </div>

          <h1>Home</h1>

          <p>
            Direct your residence in natural cadence. Orchestrate light,
            acoustics, thermal mass, and spatial security.
          </p>

        </header>


        {/* Query Section */}
        <main className="query-workspace">

          <section className="query-card">

            {/* Query heading */}
            <div className="query-heading">

              <div className="query-title">
                <span>✦</span>
                <h2>Ask your query</h2>
              </div>

              <span className="query-label">
                Natural Language Automation
              </span>

            </div>


            {/* Text Area */}
            <div className="query-input-container">

              <textarea
                value={query}
                onChange={(e) => {
                  if (e.target.value.length <= 280) {
                    setQuery(e.target.value)
                  }
                  setVisualised(false)
                }}
                placeholder="Type your query here... e.g. 'Prepare living room for evening reading and soft jazz'..."
                rows="5"
              />

              <div className="query-input-footer">

                <span>
                  〽 Encrypted Local NLP Core
                </span>

                <span>
                  {query.length} / 280
                </span>

              </div>

            </div>


            {/* Presets */}
            <div className="preset-row">

              <span className="preset-label">
                PRESETS:
              </span>

              <div className="preset-buttons">

                {presets.map((preset) => (
                  <button
                    key={preset.name}
                    onClick={() => handlePreset(preset.text)}
                  >
                    {preset.name}
                  </button>
                ))}

              </div>

            </div>

          </section>


          {/* Visualise Button */}
          <div className="visualise-container">

            <button
              className="visualise-button"
              onClick={handleVisualise}
              disabled={loading}
            >
              <span className={loading ? 'spin' : ''}>
                ✦
              </span>

              <span>
                {loading ? 'Orchestrating...' : 'VISUALISE'}
              </span>

              <span>→</span>
            </button>

          </div>


          {/* Results */}
          <section className="result-card">

            <div className="result-heading">

              <div className="result-title">
                <span className="result-dot"></span>
                <h2>Result</h2>
              </div>

              <span className="result-status">
                {loading
                  ? 'Resolving Spatial Topology...'
                  : visualised
                    ? 'Executed locally (11ms)'
                    : 'Awaiting Command'}
              </span>

            </div>


            {/* Empty State */}
            {!visualised ? (

              <div className="result-empty">

                <div className="result-icon">
                  ⌂
                </div>

                <p>
                  Your synthesized architectural execution will appear
                  here. Enter an environmental intent above.
                </p>

                <div className="telemetry-grid">

                  <div>
                    <span>AMBIENT LUX</span>
                    <strong>340 lx</strong>
                  </div>

                  <div>
                    <span>LIVING ZONE</span>
                    <strong>71.5°F</strong>
                  </div>

                  <div>
                    <span>MESH FABRIC</span>
                    <strong>Thread 100%</strong>
                  </div>

                  <div>
                    <span>LATENCY</span>
                    <strong>&lt; 14ms</strong>
                  </div>

                </div>

              </div>

            ) : (

              /* Active Result */
              <div className="active-result">

                <div className="active-result-header">

                  <div>
                    <span className="result-kicker">
                      SCENE SYNTHESIZED & STAGED
                    </span>

                    <h3>
                      {result.title}
                    </h3>
                  </div>

                  <span className="applied-badge">
                    ● APPLIED
                  </span>

                </div>


                <p className="result-description">
                  {result.description}
                </p>


                <div className="module-grid">

                  <div className="module-card">

                    <div className="module-icon">
                      💡
                    </div>

                    <div>
                      <span>ILLUMINATION</span>
                      <strong>2,700K • 32%</strong>
                    </div>

                  </div>


                  <div className="module-card">

                    <div className="module-icon">
                      ◉
                    </div>

                    <div>
                      <span>THERMAL CORE</span>
                      <strong>72.0°F • 48% RH</strong>
                    </div>

                  </div>


                  <div className="module-card">

                    <div className="module-icon">
                      ▤
                    </div>

                    <div>
                      <span>DRAPERY POSITION</span>
                      <strong>40% Sheer</strong>
                    </div>

                  </div>

                </div>


                <div className="result-footer">

                  <span>
                    🔒 Cryptographically Signed by Local Sovereign Node
                  </span>

                  <button
                    onClick={() => setVisualised(false)}
                  >
                    CLEAR STATE
                  </button>

                </div>

              </div>

            )}

          </section>

        </main>


        {/* Footer */}
        <footer className="home-footer">
          ATELIER HABITAT ARCHITECTURE • MATTER 1.3 CERTIFIED •
          0.00MS CLOUD DEPENDENCY
        </footer>

      </div>

    </div>
  )
}

export default Home