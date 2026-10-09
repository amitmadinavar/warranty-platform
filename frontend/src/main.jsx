import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { get, post, put } from './api';
import './styles.css';

const ROLE_HOME = { ADMIN: '/admin', TECHNICIAN: '/technician', CUSTOMER: '/customer' };

const NAV = {
  ADMIN: [
    { to: '/admin', label: 'Overview', icon: 'grid', end: true },
    { to: '/admin/customers', label: 'Customers', icon: 'users' },
    { to: '/admin/products', label: 'Products', icon: 'box' },
    { to: '/admin/policies', label: 'Policies', icon: 'shield' },
    { to: '/admin/warranties', label: 'Warranties', icon: 'badge' },
    { to: '/admin/requests', label: 'Service requests', icon: 'ticket' },
    { to: '/admin/repairs', label: 'Repairs & replacements', icon: 'tool' },
    { to: '/admin/analytics', label: 'Analytics', icon: 'chart' },
    { to: '/admin/audit', label: 'Audit trail', icon: 'history' }
  ],
  TECHNICIAN: [
    { to: '/technician', label: 'My workspace', icon: 'grid', end: true },
    { to: '/technician/jobs', label: 'Assigned jobs', icon: 'ticket' },
    { to: '/technician/repairs', label: 'Repair work', icon: 'tool' },
    { to: '/technician/history', label: 'Product history', icon: 'history' }
  ],
  CUSTOMER: [
    { to: '/customer', label: 'Overview', icon: 'grid', end: true },
    { to: '/customer/products', label: 'My products', icon: 'box' },
    { to: '/customer/warranties', label: 'My warranties', icon: 'badge' },
    { to: '/customer/requests', label: 'Service requests', icon: 'ticket' },
    { to: '/customer/history', label: 'Product history', icon: 'history' }
  ]
};

function Icon({ name, size = 17 }) {
  const common = { width: size, height: size, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '1.8', strokeLinecap: 'round', strokeLinejoin: 'round' };
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></>,
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2"/><circle cx="9.5" cy="7" r="3"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 4.13a3 3 0 0 1 0 5.74"/></>,
    box: <><path d="m21 8-9-5-9 5 9 5 9-5Z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/></>,
    shield: <><path d="M12 3 5 6v5c0 4.5 3 8.2 7 10 4-1.8 7-5.5 7-10V6l-7-3Z"/><path d="m9 12 2 2 4-4"/></>,
    badge: <><circle cx="12" cy="12" r="8"/><path d="m9.5 12 1.7 1.7 3.8-3.8"/><path d="m16 3 1 2 2 .5-1.5 1.5"/></>,
    ticket: <><path d="M3 7h18v4a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4V7Z"/><path d="M13 7v2"/><path d="M13 15v2"/></>,
    tool: <><path d="m14.7 6.3-7 7 3 3 7-7"/><path d="M16 3a5 5 0 0 1 5 5c0 .5-.1 1-.2 1.4l-4.3-4.3A5 5 0 0 0 16 3Z"/><path d="m5 19 2 2"/></>,
    chart: <><path d="M4 19V5"/><path d="M4 19h16"/><path d="m7 15 3-4 3 2 4-6"/></>,
    history: <><path d="M3 12a9 9 0 1 0 3-6.7"/><path d="M3 4v5h5"/><path d="M12 7v5l3 2"/></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    plus: <><path d="M12 5v14"/><path d="M5 12h14"/></>,
    logout: <><path d="M10 17l5-5-5-5"/><path d="M15 12H3"/><path d="M21 3v18"/></>,
    arrow: <><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></>,
    refresh: <><path d="M20 11a8 8 0 1 0 1 5"/><path d="M20 4v7h-7"/></>,
    check: <><path d="m5 12 4 4L19 6"/></>,
    close: <><path d="M6 6l12 12M18 6 6 18"/></>,
    menu: <><path d="M4 6h16M4 12h16M4 18h16"/></>,
    filter: <><path d="M4 5h16l-6 7v5l-4 2v-7L4 5Z"/></>
  };
  return <svg {...common}>{paths[name] || paths.grid}</svg>;
}

function useStoredUser() {
  return useMemo(() => {
    const token = localStorage.getItem('warrantyos_token');
    if (!token) return null;
    try { return JSON.parse(localStorage.getItem('warrantyos_user')) || null; } catch { return null; }
  }, []);
}

function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const user = useStoredUser();
  const navigate = useNavigate();

  useEffect(() => {
    if (user?.role && ROLE_HOME[user.role]) navigate(ROLE_HOME[user.role], { replace: true });
  }, [user, navigate]);

  const demoAccounts = {
    ADMIN: { label: 'Admin', email: 'admin@warrantyos.com', password: 'Admin@123' },
    TECHNICIAN: { label: 'Technician', email: 'tech@warrantyos.com', password: 'Tech@123' },
    CUSTOMER: { label: 'Customer', email: 'customer@warrantyos.com', password: 'Customer@123' }
  };

  const fillDemo = (role) => {
    const account = demoAccounts[role];
    setForm({ email: account.email, password: account.password });
    setError('');
  };

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const loggedInUser = await post('/auth/login', form);
      localStorage.setItem('warrantyos_user', JSON.stringify(loggedInUser));
      localStorage.setItem('warrantyos_token', loggedInUser.token);
      navigate(ROLE_HOME[loggedInUser.role] || '/', { replace: true });
    } catch (e) {
      setError(e.message || 'Unable to sign in');
    } finally {
      setLoading(false);
    }
  };

  return <div className="login-page login-page-v2">
    <section className="login-showcase">
      <div className="login-showcase-inner">
        <div className="login-brand login-brand-dark">
          <div className="brand-mark">W</div>
          <div>
            <strong>WarrantyOS</strong>
            <span>Warranty & after-sales operations</span>
          </div>
        </div>

        <div className="showcase-copy">
          <div className="login-badge login-badge-dark">AFTER-SALES OPERATIONS</div>
          <h1>One workspace for every warranty case.</h1>
          <p>Connect product coverage, customer service, technician work, repairs and replacements from one operational system.</p>
        </div>

        <div className="showcase-features">
          <div className="showcase-feature">
            <div className="feature-icon"><Icon name="shield" size={18}/></div>
            <div><strong>Warranty control</strong><span>Coverage and eligibility stay visible from registration to closure.</span></div>
          </div>
          <div className="showcase-feature">
            <div className="feature-icon"><Icon name="ticket" size={18}/></div>
            <div><strong>Service operations</strong><span>Track requests, assignments, repairs and SLA risk in one place.</span></div>
          </div>
          <div className="showcase-feature">
            <div className="feature-icon"><Icon name="chart" size={18}/></div>
            <div><strong>Operational visibility</strong><span>Give teams a clear view of customer, product and service history.</span></div>
          </div>
        </div>

        <div className="showcase-footer">
          <span><i className="status-dot"/>Live workspace</span>
          <span>Spring Boot</span>
          <span>MySQL</span>
          <span>WebSocket</span>
        </div>
      </div>
    </section>

    <section className="login-panel-v2">
      <div className="login-panel-inner">
        <div className="login-panel-head">
          <div className="login-panel-kicker">SECURE WORKSPACE</div>
          <span className="login-panel-status"><i className="status-dot"/> System ready</span>
        </div>

        <div className="login-card-v2">
          <div className="login-card-heading">
            <h2>Welcome back</h2>
            <p>Sign in to continue to your WarrantyOS workspace.</p>
          </div>

          <form onSubmit={submit} className="login-form login-form-v2">
            <label>
              <span>Email address</span>
              <input value={form.email} onChange={e=>setForm({...form,email:e.target.value})} type="email" placeholder="you@company.com" autoComplete="email" required />
            </label>
            <label>
              <span>Password</span>
              <input value={form.password} onChange={e=>setForm({...form,password:e.target.value})} type="password" placeholder="Enter your password" autoComplete="current-password" required />
            </label>
            {error && <div className="form-error form-error-v2">{error}</div>}
            <button className="btn btn-primary btn-lg login-submit" disabled={loading}>
              {loading ? 'Signing in…' : 'Sign in'}
              <Icon name="arrow" size={16}/>
            </button>
          </form>

          <div className="login-divider"><span>Development demo access</span></div>
          <div className="demo-role-grid">
            {Object.entries(demoAccounts).map(([role, account]) => (
              <button type="button" key={role} className="demo-role-card" onClick={() => fillDemo(role)}>
                <span className="demo-role-avatar">{account.label.charAt(0)}</span>
                <span><strong>{account.label}</strong><small>{account.email}</small></span>
                <Icon name="arrow" size={14}/>
              </button>
            ))}
          </div>

          <div className="login-security-note">
            <Icon name="shield" size={15}/>
            <span>Your session is secured with role-based access and JWT authentication.</span>
          </div>
        </div>

        <div className="login-panel-footer">WarrantyOS · Warranty & After-Sales Service Management</div>
      </div>
    </section>
  </div>;
}

function ProtectedLayout({ role }) {
  const user = useStoredUser();
  const navigate = useNavigate();
  const location = useLocation();
  const [noticeOpen, setNoticeOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    if (!user || user.role !== role) { navigate(ROLE_HOME[user?.role] || '/', { replace: true }); return; }
    let active = true;
    const load = async () => { try { const items = await get('/notifications'); if (active) setNotifications(items || []); } catch {} };
    load();
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'), reconnectDelay: 5000,
      onConnect: () => {
        const push = (m) => { const n = JSON.parse(m.body); setNotifications(current => [n, ...current.filter(x=>x.id !== n.id)].slice(0,50)); };
        client.subscribe(`/topic/notifications/user/${user.id}`, push);
        client.subscribe(`/topic/notifications/role/${user.role}`, push);
      }
    });
    client.activate();
    return () => { active = false; client.deactivate(); };
  }, [role]);

  if (!user || user.role !== role) return null;

  const unread = notifications.filter(n => !n.readFlag).length;
  const markRead = async (n) => {
    if (!n.readFlag) { try { await put(`/notifications/${n.id}/read`); } catch {} setNotifications(items=>items.map(x=>x.id===n.id?{...x,readFlag:true}:x)); }
  };
  const signOut = () => { localStorage.removeItem('warrantyos_user'); localStorage.removeItem('warrantyos_token'); navigate('/', { replace: true }); };
  const navItems = NAV[role];
  const activeLabel = navItems.find(n => n.to === location.pathname)?.label || navItems.find(n => location.pathname.startsWith(n.to + '/'))?.label || 'Overview';

  return <div className={`app-shell role-${role.toLowerCase()}`}>
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark">W</div><div><strong>WarrantyOS</strong><span>After-sales platform</span></div></div>
      <div className="role-context"><span className="status-dot"/>Live workspace<span className="role-tag">{role}</span></div>
      <nav className="nav"><div className="nav-section-label">Workspace</div>{navItems.map(item => <NavLink key={item.to} to={item.to} end={item.end} className={({isActive})=>`nav-item ${isActive?'active':''}`}><Icon name={item.icon}/><span>{item.label}</span></NavLink>)}</nav>
      <div className="sidebar-bottom"><div className="system-card"><div><span className="status-dot"/>Systems online</div><small>Spring Boot · MySQL · WebSocket</small></div><button className="profile-card" onClick={()=>{}}><span className="avatar">{user.name?.charAt(0)?.toUpperCase()}</span><span><strong>{user.name}</strong><small>{user.email}</small></span></button></div>
    </aside>
    <main className="main">
      <header className="topbar">
        <div><div className="breadcrumbs">WarrantyOS <span>/</span> {activeLabel}</div><h1>{activeLabel}</h1></div>
        <div className="topbar-actions">
          <button className="icon-button" onClick={()=>location.reload()} title="Refresh"><Icon name="refresh"/></button>
          <div className="notification-wrap"><button className={`icon-button ${unread?'attention':''}`} onClick={()=>setNoticeOpen(v=>!v)}><Icon name="bell"/>{unread>0&&<b>{unread>99?'99+':unread}</b>}</button>
          {noticeOpen&&<div className="notification-panel"><div className="notification-head"><div><strong>Notifications</strong><small>Live workspace activity</small></div><button onClick={()=>setNotifications([])}>Clear view</button></div>{notifications.length?notifications.map(n=><button key={n.id} className={`notification-item ${n.readFlag?'':'unread'}`} onClick={()=>markRead(n)}><span className="notification-dot"/><span><strong>{n.title}</strong><small>{n.message}</small></span></button>):<div className="notification-empty">No notifications right now.</div>}</div>}</div>
          <div className="account-chip"><span className="avatar avatar-sm">{user.name?.charAt(0)?.toUpperCase()}</span><span>{user.name}</span><em>{role}</em></div>
          <button className="btn btn-ghost" onClick={signOut}><Icon name="logout"/> Sign out</button>
        </div>
      </header>
      <div className="page-wrap"><Outlet context={{user}} /></div>
    </main>
  </div>;
}

function App() {
  return <BrowserRouter>
    <Routes>
      <Route path="/" element={<Login/>}/>
      <Route path="/admin" element={<ProtectedLayout role="ADMIN"/>}><Route index element={<AdminDashboard/>}/><Route path="customers" element={<Customers/>}/><Route path="products" element={<Products/>}/><Route path="policies" element={<Policies/>}/><Route path="warranties" element={<Warranties/>}/><Route path="requests" element={<Requests role="ADMIN"/>}/><Route path="repairs" element={<Repairs role="ADMIN"/>}/><Route path="analytics" element={<Analytics/>}/><Route path="audit" element={<Audit/>}/></Route>
      <Route path="/technician" element={<ProtectedLayout role="TECHNICIAN"/>}><Route index element={<TechnicianDashboard/>}/><Route path="jobs" element={<Requests role="TECHNICIAN"/>}/><Route path="repairs" element={<Repairs role="TECHNICIAN"/>}/><Route path="history" element={<History/>}/></Route>
      <Route path="/customer" element={<ProtectedLayout role="CUSTOMER"/>}><Route index element={<CustomerDashboard/>}/><Route path="products" element={<Products readonly/>}/><Route path="warranties" element={<Warranties readonly/>}/><Route path="requests" element={<Requests role="CUSTOMER"/>}/><Route path="history" element={<History/>}/></Route>
      <Route path="*" element={<Navigate to="/" replace/>}/>
    </Routes>
  </BrowserRouter>;
}

function PageHeader({kicker, title, text, action}) { return <div className="page-header"><div><div className="eyebrow">{kicker}</div><h2>{title}</h2><p>{text}</p></div>{action}</div>; }
function StatCard({label,value,sub,icon,tone='indigo'}) { return <div className={`stat-card tone-${tone}`}><div className="stat-top"><span className="stat-icon"><Icon name={icon}/></span><span>{label}</span></div><strong>{value}</strong><small>{sub}</small></div>; }
function Card({title,subtitle,action,children,className=''}) { return <section className={`card ${className}`}><div className="card-head"><div><h3>{title}</h3>{subtitle&&<p>{subtitle}</p>}</div>{action}</div>{children}</section>; }
function Badge({value}) { const v=String(value||'').replaceAll('_',' '); const key=String(value||'').toLowerCase(); return <span className={`badge badge-${key.replaceAll(' ','-')}`}>{v||'—'}</span>; }
function Loading(){ return <div className="loading-state"><div className="spinner"/><span>Loading workspace data…</span></div>; }
function EmptyState({title,text,action}){return <div className="empty-state"><div className="empty-mark">+</div><strong>{title}</strong><p>{text}</p>{action}</div>;}
function ErrorState({message}){return <div className="error-state"><strong>Something went wrong</strong><span>{message||'Unable to load data.'}</span></div>;}
function useFetch(loader,deps=[]){const [state,setState]=useState({loading:true,data:null,error:null});useEffect(()=>{let live=true;setState({loading:true,data:null,error:null});loader().then(data=>live&&setState({loading:false,data,error:null})).catch(error=>live&&setState({loading:false,data:null,error}));return()=>{live=false};},deps);return state;}
function Table({columns,rows,emptyTitle='No records yet',emptyText='Data will appear here as soon as activity is created.'}){ if(!rows?.length)return <EmptyState title={emptyTitle} text={emptyText}/>; return <div className="table-shell"><table><thead><tr>{columns.map(c=><th key={c.key}>{c.label}</th>)}</tr></thead><tbody>{rows.map((row,i)=><tr key={row.id??i}>{columns.map(c=><td key={c.key}>{c.render?c.render(row):row[c.key]??'—'}</td>)}</tr>)}</tbody></table></div>; }
function Field({label,value,onChange,placeholder,type='text',required=false,options,disabled=false}){return <label className="field"><span>{label}{required&&<b>*</b>}</span>{options?<select value={value} onChange={onChange} disabled={disabled}><option value="">Select…</option>{options.map(o=><option key={o} value={o}>{o.replaceAll('_',' ')}</option>)}</select>:<input value={value} onChange={onChange} placeholder={placeholder} type={type} required={required} disabled={disabled}/>}</label>;}
function Toolbar({children}){return <div className="toolbar">{children}</div>;}
function QuickAction({to,icon,title,text}){return <NavLink to={to} className="quick-action"><span><Icon name={icon}/></span><div><strong>{title}</strong><small>{text}</small></div><Icon name="arrow"/></NavLink>;}

function AdminDashboard(){
  const state = useFetch(() => Promise.all([
    get('/dashboard/summary'),
    get('/analytics'),
    get('/escalations')
  ]), []);

  if (state.loading) return <Loading/>;
  if (state.error) return <ErrorState message={state.error.message}/>;

  const [d, a, e] = state.data;
  const recent = d.recentRequests || [];
  const mix = Object.entries(a.byStatus || {});
  const max = Math.max(1, ...mix.map(([, value]) => Number(value)));

  return <>
    <PageHeader
      kicker="Operations control"
      title="Warranty operations, at a glance."
      text="Monitor demand, coverage, service workload and SLA pressure from one operational view."
      action={<div className="live-pill"><span/>All systems operational</div>}
    />

    <div className="stats-grid">
      <StatCard label="Customers" value={d.customers} sub="Active accounts" icon="users"/>
      <StatCard label="Registered products" value={d.products} sub="Tracked serials" icon="box" tone="cyan"/>
      <StatCard label="Active warranties" value={d.activeWarranties} sub="Currently covered" icon="badge" tone="green"/>
      <StatCard label="Open service cases" value={d.openRequests} sub={`${d.escalated} at SLA risk`} icon="ticket" tone="amber"/>
    </div>

    <div className="dashboard-grid admin-dashboard-grid">
      <Card title="Service workload" subtitle="Live distribution by case status" className="span-2">
        <div className="bar-chart">
          {mix.length > 0 ? mix.map(([name, value]) => (
            <div className="bar-row" key={name}>
              <div className="bar-label">
                <span>{name.replaceAll('_', ' ')}</span>
                <strong>{value}</strong>
              </div>
              <div className="bar-track">
                <i style={{ width: `${Number(value) / max * 100}%` }}/>
              </div>
            </div>
          )) : (
            <EmptyState title="No requests yet" text="Create your first service request to activate this view."/>
          )}
        </div>
      </Card>

      <Card title="SLA watch" subtitle="Cases requiring immediate attention">
        <div className="big-stat danger">{e.length}</div>
        <div className="muted">Escalated cases</div>
        {e.slice(0, 3).map(r => (
          <div className="alert-row" key={r.id}>
            <span>!</span>
            <div><strong>Request #{r.id}</strong><small>{r.issue}</small></div>
            <Badge value={r.status}/>
          </div>
        ))}
        {!e.length && <div className="success-callout">No SLA breaches. Operations are on track.</div>}
      </Card>

      <Card title="Recent service activity" subtitle="Latest cases across the network" className="span-2">
        <Table rows={recent} columns={[
          {key:'id', label:'CASE', render:r=><strong>#{r.id}</strong>},
          {key:'serialNumber', label:'SERIAL'},
          {key:'issue', label:'ISSUE'},
          {key:'status', label:'STATUS', render:r=><Badge value={r.status}/>},
          {key:'assignedTo', label:'ASSIGNED'}
        ]}/>
      </Card>

      <Card title="Quick actions" subtitle="Common operations">
        <div className="quick-grid">
          <QuickAction to="/admin/requests" icon="ticket" title="Manage requests" text="Assign and update cases"/>
          <QuickAction to="/admin/products" icon="box" title="Register product" text="Add a new serial"/>
          <QuickAction to="/admin/warranties" icon="badge" title="Activate warranty" text="Start customer coverage"/>
          <QuickAction to="/admin/analytics" icon="chart" title="View analytics" text="Review trends and SLAs"/>
        </div>
      </Card>
    </div>
  </>;
}

function TechnicianDashboard(){
  const state=useFetch(()=>get('/dashboard/summary'),[]); if(state.loading)return <Loading/>; if(state.error)return <ErrorState message={state.error.message}/>; const d=state.data||{}; const jobs=d.recentRequests||[];
  return <>
    <PageHeader kicker="Field service workspace" title="Focus on the jobs that need action." text="See today's workload, SLA risk and the next repair decision without the noise of the admin console." action={<div className="live-pill"><span/>Live job queue</div>}/>
    <div className="stats-grid"><StatCard label="Assigned jobs" value={d.assigned} sub="Your current workload" icon="ticket"/><StatCard label="Open work" value={d.open} sub="Cases needing action" icon="tool" tone="amber"/><StatCard label="Resolved" value={d.resolved} sub="Completed successfully" icon="check" tone="green"/><StatCard label="SLA risk" value={d.escalated} sub="Needs escalation review" icon="shield" tone="red"/></div>
    <div className="dashboard-grid">
      <Card title="My priority queue" subtitle="Work closest to SLA or customer impact" className="span-2"><Table rows={jobs} columns={[{key:'id',label:'CASE',render:r=><strong>#{r.id}</strong>},{key:'serialNumber',label:'PRODUCT'},{key:'issue',label:'ISSUE'},{key:'status',label:'STATUS',render:r=><Badge value={r.status}/>},{key:'escalated',label:'SLA',render:r=>r.escalated?<Badge value="ESCALATED"/>:<span className="on-track">On track</span>}]}/></Card>
      <Card title="Shift checklist" subtitle="Keep customer visibility current"><div className="checklist"><div><Icon name="check"/><span>Update request status after each field action.</span></div><div><Icon name="check"/><span>Record diagnosis and parts used in repairs.</span></div><div><Icon name="check"/><span>Resolve or escalate before the SLA window closes.</span></div></div><NavLink to="/technician/repairs" className="btn btn-secondary full">Open repair workspace <Icon name="arrow"/></NavLink></Card>
    </div>
  </>;
}

function CustomerDashboard(){
  const state=useFetch(()=>Promise.all([get('/dashboard/summary'),get('/products'),get('/warranties'),get('/requests')]),[]); if(state.loading)return <Loading/>; if(state.error)return <ErrorState message={state.error.message}/>; const [d,products,warranties,requests]=state.data;
  const activeW = warranties.filter(w=>w.status==='ACTIVE').length;
  return <>
    <PageHeader kicker="Customer service portal" title={`Welcome back, ${d.role==='CUSTOMER'?'Customer':''}.`} text="See your products, coverage and service journey in one place." action={<NavLink to="/customer/requests" className="btn btn-primary">Create service request <Icon name="arrow"/></NavLink>}/>
    <div className="stats-grid"><StatCard label="My products" value={products.length} sub="Registered with WarrantyOS" icon="box"/><StatCard label="Active coverage" value={activeW} sub="Warranties in force" icon="badge" tone="green"/><StatCard label="Open requests" value={d.openRequests} sub="Still in progress" icon="ticket" tone="amber"/><StatCard label="Resolved" value={d.resolvedRequests} sub="Completed service cases" icon="check" tone="cyan"/></div>
    <div className="dashboard-grid customer-grid">
      <Card title="My products" subtitle="Your registered assets" className="span-2"><div className="product-grid">{products.slice(0,4).map(p=><div className="product-card" key={p.id}><div className="product-icon"><Icon name="box"/></div><div><strong>{p.model}</strong><span>{p.serialNumber}</span><small>{p.category} · Purchased {p.purchaseDate}</small></div></div>)}{!products.length&&<EmptyState title="No products registered" text="Contact support or ask an administrator to register your product."/>}</div></Card>
      <Card title="Coverage snapshot" subtitle="Active warranties"><div className="coverage-list">{warranties.slice(0,4).map(w=><div key={w.id}><div><strong>{w.serialNumber}</strong><small>Expires {w.expiryDate}</small></div><Badge value={w.status}/></div>)}{!warranties.length&&<EmptyState title="No warranties" text="Warranty coverage will appear here once a product is activated."/>}</div></Card>
      <Card title="Recent service activity" subtitle="Your latest cases"><Table rows={requests.slice(0,5)} columns={[{key:'id',label:'CASE',render:r=><strong>#{r.id}</strong>},{key:'serialNumber',label:'SERIAL'},{key:'issue',label:'ISSUE'},{key:'status',label:'STATUS',render:r=><Badge value={r.status}/>},{key:'assignedTo',label:'TECHNICIAN'}]}/></Card>
    </div>
  </>;
}

function Customers(){
  const state=useFetch(()=>get('/customers'),[]); const [form,setForm]=useState({name:'',email:'',phone:''}); const [q,setQ]=useState(''); if(state.loading)return <Loading/>; if(state.error)return <ErrorState message={state.error.message}/>; const rows=state.data.filter(c=>`${c.name} ${c.email} ${c.phone}`.toLowerCase().includes(q.toLowerCase()));
  const save=async e=>{e.preventDefault();await post('/customers',form);setForm({name:'',email:'',phone:''});location.reload();};
  return <><PageHeader kicker="Customer management" title="Customer accounts" text="Manage the people, contact details and service footprint behind every case." action={<span className="count-chip">{state.data.length} accounts</span>}/><div className="content-grid two-cols"><Card title="Add customer" subtitle="Create an account record for after-sales operations"><form onSubmit={save} className="form-grid"><Field label="Full name" value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required placeholder="Customer full name"/><Field label="Email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} type="email" required placeholder="customer@example.com"/><Field label="Phone" value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})} placeholder="+91 …"/><button className="btn btn-primary">Create customer <Icon name="plus"/></button></form></Card><Card title="Customer directory" subtitle="Search by name, email or phone"><Toolbar><div className="search-box"><Icon name="search"/><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Search customers"/></div></Toolbar><Table rows={rows} columns={[{key:'id',label:'ID'},{key:'name',label:'CUSTOMER',render:r=><strong>{r.name}</strong>},{key:'email',label:'EMAIL'},{key:'phone',label:'PHONE'}]}/></Card></div></>;
}

function Products({readonly=false}){
  const state=useFetch(()=>get('/products'),[]); const cust=useFetch(()=>readonly?Promise.resolve([]):get('/customers'),[readonly]); const [form,setForm]=useState({serialNumber:'',model:'',category:'',purchaseDate:'',customerId:''}); if(state.loading||cust.loading&&!readonly)return <Loading/>; if(state.error)return <ErrorState message={state.error.message}/>; const save=async e=>{e.preventDefault();await post('/products',{...form,customerId:Number(form.customerId),purchaseDate:form.purchaseDate});location.reload();};
  return <><PageHeader kicker={readonly?'Customer assets':'Asset registry'} title={readonly?'My products':'Products'} text={readonly?'Your registered assets and the serials linked to your account.':'Register and trace every product entering the warranty lifecycle.'}/><div className="content-grid"><Card title="Registered products" subtitle={`${state.data.length} serials in the system`}><Table rows={state.data} columns={[{key:'serialNumber',label:'SERIAL',render:r=><strong>{r.serialNumber}</strong>},{key:'model',label:'MODEL'},{key:'category',label:'CATEGORY'},{key:'purchaseDate',label:'PURCHASE DATE'},{key:'customerId',label:'CUSTOMER ID'}]}/></Card>{!readonly&&<Card title="Register product" subtitle="Link a serial to a customer account"><form onSubmit={save} className="form-grid"><Field label="Serial number" value={form.serialNumber} onChange={e=>setForm({...form,serialNumber:e.target.value})} required placeholder="SN-10024"/><Field label="Model" value={form.model} onChange={e=>setForm({...form,model:e.target.value})} required placeholder="Samsung QLED"/><Field label="Category" value={form.category} onChange={e=>setForm({...form,category:e.target.value})} required placeholder="TV"/><Field label="Purchase date" value={form.purchaseDate} onChange={e=>setForm({...form,purchaseDate:e.target.value})} type="date" required/><Field label="Customer" value={form.customerId} onChange={e=>setForm({...form,customerId:e.target.value})} options={(cust.data||[]).map(c=>String(c.id))} required/><button className="btn btn-primary">Register product <Icon name="plus"/></button></form></Card>}</div></>;
}

function Policies(){const state=useFetch(()=>get('/policies'),[]); const [form,setForm]=useState({name:'',category:'',model:'',durationMonths:'12',exclusions:'',replaceAfterRepairs:'3'});if(state.loading)return <Loading/>;if(state.error)return <ErrorState message={state.error.message}/>;const save=async e=>{e.preventDefault();await post('/policies',{...form,durationMonths:Number(form.durationMonths),replaceAfterRepairs:Number(form.replaceAfterRepairs),active:true});location.reload();};return <><PageHeader kicker="Coverage design" title="Warranty policies" text="Define coverage duration, exclusions and replacement thresholds used by the warranty engine."/><div className="content-grid"><Card title="Policy catalogue"><Table rows={state.data} columns={[{key:'id',label:'ID'},{key:'name',label:'POLICY',render:r=><strong>{r.name}</strong>},{key:'category',label:'CATEGORY'},{key:'model',label:'MODEL'},{key:'durationMonths',label:'TERM'},{key:'replaceAfterRepairs',label:'REPLACE AFTER'}]}/></Card><Card title="Create policy" subtitle="Keep coverage rules explicit"><form onSubmit={save} className="form-grid"><Field label="Policy name" value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required placeholder="Premium protection"/><Field label="Category" value={form.category} onChange={e=>setForm({...form,category:e.target.value})} required placeholder="TV"/><Field label="Model" value={form.model} onChange={e=>setForm({...form,model:e.target.value})} placeholder="Model-specific rule"/><Field label="Duration (months)" value={form.durationMonths} onChange={e=>setForm({...form,durationMonths:e.target.value})} type="number"/><Field label="Exclusions" value={form.exclusions} onChange={e=>setForm({...form,exclusions:e.target.value})} placeholder="physical damage,liquid damage"/><Field label="Replacement threshold" value={form.replaceAfterRepairs} onChange={e=>setForm({...form,replaceAfterRepairs:e.target.value})} type="number"/><button className="btn btn-primary">Save policy <Icon name="check"/></button></form></Card></div></>}

function Warranties({readonly=false}){const state=useFetch(()=>get('/warranties'),[]);const prod=useFetch(()=>get('/products'),[]);const [serial,setSerial]=useState('');if(state.loading||prod.loading)return <Loading/>;if(state.error)return <ErrorState message={state.error.message}/>;const owned=new Set(prod.data.map(p=>p.serialNumber));const rows=readonly?state.data.filter(w=>owned.has(w.serialNumber)):state.data;return <><PageHeader kicker="Warranty coverage" title={readonly?'My warranties':'Warranty register'} text={readonly?'Review your current protection, expiry dates and claim eligibility.':'Activate and monitor warranty coverage against registered serial numbers.'}/><div className="content-grid"><Card title="Coverage register" subtitle={`${rows.length} coverage records`}><Table rows={rows} columns={[{key:'serialNumber',label:'SERIAL',render:r=><strong>{r.serialNumber}</strong>},{key:'activationDate',label:'ACTIVATED'},{key:'expiryDate',label:'EXPIRES'},{key:'status',label:'STATUS',render:r=><Badge value={r.status}/>}]}/></Card>{!readonly&&<Card title="Activate warranty" subtitle="Activation uses the applicable policy"><form onSubmit={async e=>{e.preventDefault();await post(`/warranties/activate/${encodeURIComponent(serial)}`);setSerial('');location.reload();}} className="form-grid"><Field label="Product serial" value={serial} onChange={e=>setSerial(e.target.value)} required placeholder="TV-10001"/><button className="btn btn-primary">Activate coverage <Icon name="badge"/></button></form><div className="helper-box">The warranty engine checks model/category policy rules automatically.</div></Card>}</div></>}

function Requests({role}){
  const state=useFetch(()=>get('/requests'),[role]); const techs=useFetch(()=>role==='ADMIN'?get('/users/technicians'):Promise.resolve([]),[role]); const [form,setForm]=useState({serialNumber:'',issue:''}); if(state.loading||techs.loading)return <Loading/>; if(state.error)return <ErrorState message={state.error.message}/>;
  const update=async(id,status)=>{await put(`/requests/${id}/status?status=${encodeURIComponent(status)}`);location.reload();};
  const assign=async(id,tech)=>{await put(`/requests/${id}/assign?tech=${encodeURIComponent(tech)}`);location.reload();};
  const create=async e=>{e.preventDefault();await post('/requests',form);setForm({serialNumber:'',issue:''});location.reload();};
  return <><PageHeader kicker={role==='CUSTOMER'?'Customer service':'Service operations'} title={role==='CUSTOMER'?'My service requests':role==='TECHNICIAN'?'Assigned jobs':'Service request command centre'} text={role==='CUSTOMER'?'Raise, track and understand every case linked to your products.':role==='TECHNICIAN'?'Update your assigned jobs and keep the customer informed.':'Control intake, assignment, SLA exposure and resolution across the network.'}/>
    {role==='CUSTOMER'&&<Card title="Create service request" subtitle="Coverage eligibility is checked automatically"><form onSubmit={create} className="request-form"><div className="request-grid"><Field label="Product serial" value={form.serialNumber} onChange={e=>setForm({...form,serialNumber:e.target.value})} required placeholder="TV-10001"/><label className="field field-wide"><span>Issue<b>*</b></span><textarea value={form.issue} onChange={e=>setForm({...form,issue:e.target.value})} placeholder="Describe the issue, symptoms or error message" required/></label></div><button className="btn btn-primary">Submit service request <Icon name="arrow"/></button></form></Card>}
    <Card title={role==='TECHNICIAN'?'My work queue':'Service queue'} subtitle={`${state.data.length} cases visible`}><Table rows={state.data} columns={[{key:'id',label:'CASE',render:r=><strong>#{r.id}</strong>},{key:'serialNumber',label:'SERIAL'},{key:'issue',label:'ISSUE'},{key:'status',label:'STATUS',render:r=><Badge value={r.status}/>},{key:'assignedTo',label:'ASSIGNED'},{key:'eligibility',label:'COVERAGE',render:r=><Badge value={r.eligibility}/>},{key:'actions',label:'ACTION',render:r=>role==='TECHNICIAN'?<div className="row-actions">{r.status==='ASSIGNED'&&<button onClick={()=>update(r.id,'UNDER_REPAIR')} className="mini-btn">Start</button>}{r.status==='UNDER_REPAIR'&&<button onClick={()=>update(r.id,'RESOLVED')} className="mini-btn success">Resolve</button>}</div>:role==='ADMIN'?<div className="row-actions">{r.status!=='CLOSED'&&<select value="" onChange={e=>{if(e.target.value)assign(r.id,e.target.value)}}><option value="">Assign…</option>{techs.data.map(t=><option value={t.name} key={t.id}>{t.name}</option>)}</select>}</div>:<span className="muted">Live tracking</span>}]}/></Card>
  </>;
}

function Repairs({role}){const state=useFetch(()=>Promise.all([get('/repairs'),role==='ADMIN'||role==='TECHNICIAN'?get('/replacements'):Promise.resolve([])]),[role]);const [form,setForm]=useState({requestId:'',diagnosis:'',action:'',parts:'',laborCost:''});if(state.loading)return <Loading/>;if(state.error)return <ErrorState message={state.error.message}/>;const [repairs,replacements]=state.data;const save=async e=>{e.preventDefault();await post('/repairs',{...form,requestId:Number(form.requestId),laborCost:Number(form.laborCost||0)});location.reload();};return <><PageHeader kicker="After-sales execution" title={role==='TECHNICIAN'?'Repair workspace':'Repairs & replacements'} text={role==='TECHNICIAN'?'Record diagnosis, parts and completed work against your assigned cases.':'Review completed work and replacement outcomes across the service network.'}/><div className="stats-grid"><StatCard label="Repairs logged" value={repairs.length} sub="Recorded work orders" icon="tool"/><StatCard label="Replacements" value={replacements.length} sub="Processed units" icon="box" tone="cyan"/><StatCard label="Service actions" value={repairs.length+replacements.length} sub="Operational history" icon="history" tone="green"/><StatCard label="Data source" value="Live" sub="MySQL" icon="check" tone="indigo"/></div><div className="content-grid">{role==='TECHNICIAN'&&<Card title="Log repair" subtitle="Completing a repair resolves the linked request"><form onSubmit={save} className="form-grid"><Field label="Request ID" value={form.requestId} onChange={e=>setForm({...form,requestId:e.target.value})} type="number" required placeholder="104"/><Field label="Diagnosis" value={form.diagnosis} onChange={e=>setForm({...form,diagnosis:e.target.value})} placeholder="Diagnosis"/><Field label="Action" value={form.action} onChange={e=>setForm({...form,action:e.target.value})} placeholder="Action taken"/><Field label="Parts used" value={form.parts} onChange={e=>setForm({...form,parts:e.target.value})} placeholder="Main board"/><Field label="Labour cost" value={form.laborCost} onChange={e=>setForm({...form,laborCost:e.target.value})} type="number"/><button className="btn btn-primary">Record repair <Icon name="check"/></button></form></Card>}<Card title="Repair history" subtitle={`${repairs.length} recorded repairs`} className={role==='ADMIN'?'span-2':''}><Table rows={repairs} columns={[{key:'id',label:'ID'},{key:'requestId',label:'CASE'},{key:'diagnosis',label:'DIAGNOSIS'},{key:'action',label:'ACTION'},{key:'parts',label:'PARTS'},{key:'laborCost',label:'LABOUR'}]}/></Card></div>{role==='ADMIN'&&<Card title="Replacement history" subtitle={`${replacements.length} processed replacements`}><Table rows={replacements} columns={[{key:'id',label:'ID'},{key:'requestId',label:'CASE'},{key:'originalSerial',label:'ORIGINAL'},{key:'replacementSerial',label:'REPLACEMENT'},{key:'reason',label:'REASON'}]}/></Card>}</>}

function History(){const [serial,setSerial]=useState('');const [data,setData]=useState(null);const [error,setError]=useState('');const search=async e=>{e.preventDefault();setError('');try{setData(await get(`/history/${encodeURIComponent(serial)}`));}catch(err){setError(err.message)}};return <><PageHeader kicker="Traceability" title="Product history" text="Follow one serial number from purchase to service, repair and replacement."/><Card title="Search product lifecycle" subtitle="Use the exact registered serial"><form onSubmit={search} className="search-form"><div className="search-box large"><Icon name="search"/><input value={serial} onChange={e=>setSerial(e.target.value)} placeholder="Enter serial number" required/></div><button className="btn btn-primary">Search history <Icon name="arrow"/></button></form></Card>{error&&<ErrorState message={error}/>} {data&&<div className="history-grid"><Card title="Service requests"><Table rows={data.requests||[]} columns={[{key:'id',label:'CASE',render:r=><strong>#{r.id}</strong>},{key:'issue',label:'ISSUE'},{key:'status',label:'STATUS',render:r=><Badge value={r.status}/>},{key:'createdAt',label:'CREATED',render:r=>String(r.createdAt||'').replace('T',' ').slice(0,16)}]}/></Card><Card title="Repairs"><Table rows={data.repairs||[]} columns={[{key:'requestId',label:'CASE'},{key:'diagnosis',label:'DIAGNOSIS'},{key:'action',label:'ACTION'},{key:'parts',label:'PARTS'}]}/></Card><Card title="Replacements"><Table rows={data.replacements||[]} columns={[{key:'requestId',label:'CASE'},{key:'originalSerial',label:'ORIGINAL'},{key:'replacementSerial',label:'REPLACEMENT'},{key:'reason',label:'REASON'}]}/></Card></div>}</>}

function Analytics(){
  const state=useFetch(()=>Promise.all([get('/analytics'),get('/escalations')]),[]);
  if(state.loading)return <Loading/>;
  if(state.error)return <ErrorState message={state.error.message}/>;

  const [a,e]=state.data;
  const entries=Object.entries(a.byStatus||{});
  const max=Math.max(1,...entries.map(([,v])=>Number(v)));
  const modelEntries=Object.entries(a.requestsByModel||{});

  return <>
    <PageHeader
      kicker="Operations intelligence"
      title="Service analytics"
      text="Understand demand, turnaround, model concentration and SLA pressure."
    />

    <div className="stats-grid">
      <StatCard label="Total requests" value={a.totalRequests} sub="All-time service cases" icon="ticket"/>
      <StatCard label="Replacement rate" value={`${a.replacementRatePct}%`} sub="Requests ending in replacement" icon="box" tone="cyan"/>
      <StatCard label="Avg turnaround" value={`${a.avgTurnaroundHours}h`} sub="Resolved cases" icon="history" tone="green"/>
      <StatCard label="Escalated" value={a.escalated} sub="Past SLA" icon="shield" tone="red"/>
    </div>

    <div className="dashboard-grid">
      <Card title="Request mix" subtitle="Status distribution" className="span-2">
        <div className="bar-chart">
          {entries.map(([name,value])=>(
            <div className="bar-row" key={name}>
              <div className="bar-label">
                <span>{name.replaceAll('_',' ')}</span>
                <strong>{value}</strong>
              </div>
              <div className="bar-track">
                <i style={{width:`${(Number(value)/max)*100}%`}} />
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Card title="Top models" subtitle="Request concentration">
        <div className="model-list">
          {modelEntries.length === 0 ? (
            <EmptyState title="No model data" text="Service request analytics will appear here once cases are recorded." />
          ) : (
            modelEntries.map(([name,value])=>(
              <div className="model-row" key={name}>
                <div className="model-row-head">
                  <strong>{name}</strong>
                  <span>{value} cases</span>
                </div>
                <div className="progress">
                  <i style={{width:`${Math.min(100,Number(value)*12)}%`}} />
                </div>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>

    <Card title="Escalation watch" subtitle={`${e.length} cases beyond the SLA window`}>
      <Table
        rows={e}
        columns={[
          {key:'id',label:'CASE',render:r=><strong>#{r.id}</strong>},
          {key:'serialNumber',label:'SERIAL'},
          {key:'issue',label:'ISSUE'},
          {key:'status',label:'STATUS',render:r=><Badge value={r.status}/>},
          {key:'assignedTo',label:'ASSIGNED'}
        ]}
      />
    </Card>
  </>;
}

function Audit(){const state=useFetch(()=>get('/audit'),[]);const [q,setQ]=useState('');if(state.loading)return <Loading/>;if(state.error)return <ErrorState message={state.error.message}/>;const rows=state.data.filter(r=>`${r.entity} ${r.action} ${r.detail}`.toLowerCase().includes(q.toLowerCase()));return <><PageHeader kicker="Governance" title="Audit trail" text="Traceable operational history for customer, warranty and service changes."/><Card title="System audit log" subtitle={`${rows.length} matching events`}><Toolbar><div className="search-box"><Icon name="search"/><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Search action, entity or detail"/></div></Toolbar><Table rows={rows} columns={[{key:'at',label:'TIME',render:r=>String(r.at||'').replace('T',' ').slice(0,19)},{key:'entity',label:'ENTITY'},{key:'entityId',label:'ID'},{key:'action',label:'ACTION',render:r=><Badge value={r.action}/>},{key:'detail',label:'DETAIL'}]}/></Card></>}

createRoot(document.getElementById('root')).render(<App/>);
