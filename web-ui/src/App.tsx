import { StopsMap } from './components/StopsMap'
import './App.css'

function App() {
  return (
    <main className="app-shell">
      <header className="hero-header">
        <div>
          <p className="eyebrow">Budapest transport demo</p>
          <h1>budaTravel map MVP</h1>
          <p>
            Minimal web UI for exploring stops imported from the backend demo dataset. Move the map to
            request a fresh bounding box from the API.
          </p>
        </div>
      </header>
      <StopsMap />
    </main>
  )
}

export default App
