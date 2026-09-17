import { useState } from 'react'
import Authentication from './pages/Authentication'
import Landing from './pages/Landing'

function App() {
  const [authenticated, setAuthenticated] = useState(false)

  if (!authenticated) {
    return (
      <Authentication
        onLogin={() => setAuthenticated(true)}
      />
    )
  }

  return <Landing />
}

export default App