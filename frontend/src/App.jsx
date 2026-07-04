import Dashboard from './pages/Dashboard'
import ImportPage from './pages/ImportPage'

export default function App() {
  return window.location.pathname.startsWith('/import')
    ? <ImportPage />
    : <Dashboard />
}
