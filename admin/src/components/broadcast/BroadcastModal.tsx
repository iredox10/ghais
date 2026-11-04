import React, { useState, useEffect } from 'react';
import { Megaphone, Smartphone, Send, AlertCircle, CheckCircle2, Clock, Users, Sparkles, Bell } from 'lucide-react';
import { Modal, ModalHeader, ModalTitle, ModalDescription, ModalContent, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { sendBroadcast, getBroadcasts, deleteBroadcast } from '../../api/adminClient';

export interface BroadcastModalProps {
  isOpen: boolean;
  onClose: () => void;
  onDispatched?: () => void;
}

const URGENCY_OPTIONS = [
  { label: 'Normal', color: 'bg-blue-500/20 text-blue-400 border-blue-500/30' },
  { label: 'Urgent', color: 'bg-rose-500/20 text-rose-400 border-rose-500/30' },
  { label: 'Friday Reminder', color: 'bg-amber-500/20 text-amber-400 border-amber-500/30' },
];

const TARGET_GROUPS = [
  { label: 'All Listeners', desc: 'Broadcast to all active app installations across Android & iOS' },
  { label: 'Daily Khatma Streaks', desc: 'Target users maintaining active Quran reading streaks' },
  { label: 'Jumu\'ah Surah Al-Kahf Readers', desc: 'Subscribers who listen to Friday recitations' },
];

export const BroadcastModal: React.FC<BroadcastModalProps> = ({ isOpen, onClose, onDispatched }) => {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [urgency, setUrgency] = useState('Normal');
  const [targetTopic, setTargetTopic] = useState('All Listeners');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successResult, setSuccessResult] = useState<{ broadcastId: string; timestamp: string } | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [broadcasts, setBroadcasts] = useState<
    { id: string; title: string; body: string; audience?: string; urgency?: string; timestamp?: string }[]
  >([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const loadHistory = async () => {
    setHistoryLoading(true);
    setHistoryError(null);
    try {
      const res = await getBroadcasts();
      if (res.success) {
        setBroadcasts(res.broadcasts ?? []);
      } else {
        setHistoryError('Failed to load broadcast history.');
      }
    } catch (err: any) {
      setHistoryError(err.message || 'Error loading broadcast history.');
    } finally {
      setHistoryLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadHistory();
    }
  }, [isOpen]);

  const handleDelete = async (id: string) => {
    setDeletingId(id);
    try {
      await deleteBroadcast(id);
      await loadHistory();
    } catch (err: any) {
      setHistoryError(err.message || 'Error deleting broadcast.');
    } finally {
      setDeletingId(null);
    }
  };

  const resetForm = () => {
    setTitle('');
    setBody('');
    setUrgency('Normal');
    setTargetTopic('All Listeners');
    setSuccessResult(null);
    setErrorMsg(null);
    setShowConfirm(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleDispatchSubmit = async () => {
    if (!title.trim() || !body.trim()) {
      setErrorMsg('Please enter both a title and message body.');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await sendBroadcast({
        title: title.trim(),
        body: body.trim(),
        urgency,
        targetTopic,
      });

      if (res.success) {
        setSuccessResult({
          broadcastId: res.broadcastId,
          timestamp: res.timestamp,
        });
        setShowConfirm(false);
        if (onDispatched) onDispatched();
        loadHistory();
      } else {
        setErrorMsg('Failed to dispatch broadcast.');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Error dispatching broadcast.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="xl" title={null} showCloseButton={false}>
      {/* Custom Modal Header */}
      <div className="flex items-center justify-between p-6 pb-4 border-b border-white/[0.08] bg-white/[0.02]">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
            <Megaphone className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-xl font-semibold text-white tracking-tight flex items-center gap-2">
              Broadcast & Push Dispatcher
              <Badge variant="emerald" className="text-[10px] font-mono">Live FCM Gateway</Badge>
            </h2>
            <p className="text-xs text-white/50">
              Compose in-app announcements and push notifications for Ghais listeners.
            </p>
          </div>
        </div>
        <button
          onClick={handleClose}
          className="rounded-xl p-2 text-white/40 hover:text-white hover:bg-white/10 transition-colors"
        >
          ✕
        </button>
      </div>

      <div className="p-6 grid grid-cols-1 lg:grid-cols-12 gap-6 max-h-[75vh] overflow-y-auto">
        {/* Left Column: Form Controls */}
        <div className="lg:col-span-7 space-y-5">
          {successResult ? (
            <div className="p-6 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-center space-y-4 animate-fade-in">
              <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 mx-auto flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-white">Broadcast Dispatched Successfully!</h3>
                <p className="text-xs text-white/60 mt-1">
                  ID: <span className="font-mono text-emerald-400">{successResult.broadcastId}</span>
                </p>
                <p className="text-[11px] text-white/40 font-mono mt-0.5">
                  Timestamp: {new Date(successResult.timestamp).toLocaleString()}
                </p>
              </div>
              <div className="flex justify-center gap-3 pt-2">
                <Button variant="glass" onClick={resetForm} className="text-xs">
                  Compose Another
                </Button>
                <Button variant="chrome" onClick={handleClose} className="text-xs bg-emerald-500 hover:bg-emerald-600 text-black font-medium">
                  Done
                </Button>
              </div>
            </div>
          ) : (
            <>
              {errorMsg && (
                <div className="flex items-center gap-2.5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{errorMsg}</span>
                </div>
              )}

              {/* Title Input */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-white/80">Announcement Title</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Surah Al-Kahf Friday Reminder"
                  className="w-full px-3.5 py-2.5 bg-black/40 border border-white/[0.1] rounded-xl text-xs text-white placeholder-white/30 focus:outline-none focus:border-emerald-500 transition-colors"
                />
              </div>

              {/* Message Body */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-white/80">Message Body</label>
                <textarea
                  rows={4}
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                  placeholder="Write announcement or push notification message here..."
                  className="w-full px-3.5 py-2.5 bg-black/40 border border-white/[0.1] rounded-xl text-xs text-white placeholder-white/30 focus:outline-none focus:border-emerald-500 transition-colors resize-none"
                />
              </div>

              {/* Urgency Selector */}
              <div className="space-y-2">
                <label className="text-xs font-medium text-white/80 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
                  Urgency Badge
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {URGENCY_OPTIONS.map((opt) => (
                    <button
                      key={opt.label}
                      type="button"
                      onClick={() => setUrgency(opt.label)}
                      className={`px-3 py-2 rounded-xl text-xs font-medium border transition-all flex items-center justify-center gap-1.5 ${
                        urgency === opt.label
                          ? `${opt.color} ring-2 ring-emerald-500/50 shadow-sm`
                          : 'bg-white/[0.03] text-white/60 border-white/[0.08] hover:bg-white/[0.06] hover:text-white'
                      }`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${urgency === opt.label ? 'bg-emerald-400 animate-pulse' : 'bg-white/30'}`} />
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Target Group Selector */}
              <div className="space-y-2">
                <label className="text-xs font-medium text-white/80 flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-emerald-400" />
                  Target Audience Group
                </label>
                <div className="space-y-2">
                  {TARGET_GROUPS.map((group) => (
                    <div
                      key={group.label}
                      onClick={() => setTargetTopic(group.label)}
                      className={`p-3 rounded-xl border cursor-pointer transition-all flex items-start gap-3 ${
                        targetTopic === group.label
                          ? 'bg-emerald-500/10 border-emerald-500/40 text-white'
                          : 'bg-black/20 border-white/[0.06] text-white/70 hover:border-white/20'
                      }`}
                    >
                      <input
                        type="radio"
                        name="targetGroup"
                        checked={targetTopic === group.label}
                        onChange={() => setTargetTopic(group.label)}
                        className="mt-0.5 accent-emerald-500 cursor-pointer"
                      />
                      <div>
                        <div className="text-xs font-medium text-white">{group.label}</div>
                        <div className="text-[11px] text-white/50">{group.desc}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Confirmation Step / Dispatch Button */}
              <div className="pt-2">
                {showConfirm ? (
                  <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 space-y-3">
                    <p className="text-xs font-medium text-amber-200">
                      Are you sure you want to broadcast this announcement to <span className="font-bold underline">{targetTopic}</span>?
                    </p>
                    <div className="flex gap-2">
                      <Button
                        variant="chrome"
                        onClick={handleDispatchSubmit}
                        disabled={isSubmitting}
                        className="flex-1 bg-emerald-500 hover:bg-emerald-600 text-black text-xs font-semibold"
                      >
                        {isSubmitting ? 'Dispatching...' : 'Yes, Confirm & Dispatch'}
                      </Button>
                      <Button
                        variant="glass"
                        onClick={() => setShowConfirm(false)}
                        className="text-xs border-white/20 text-white/80 hover:bg-white/10"
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <Button
                    variant="chrome"
                    onClick={() => setShowConfirm(true)}
                    className="w-full py-3 bg-gradient-to-r from-emerald-500 to-emerald-400 text-black font-semibold text-xs hover:from-emerald-400 hover:to-emerald-300 shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
                  >
                    <Send className="w-4 h-4" />
                    Dispatch to App Now
                  </Button>
                )}
              </div>
            </>
          )}
        </div>

        {/* Right Column: Mobile Preview Container */}
        <div className="lg:col-span-5 flex flex-col items-center justify-center bg-black/40 rounded-2xl border border-white/[0.08] p-6">
          <div className="flex items-center gap-2 text-xs font-medium text-white/60 mb-4">
            <Smartphone className="w-4 h-4 text-emerald-400" />
            <span>Mobile Push Notification Preview</span>
          </div>

          {/* Smartphone Bezel Mockup */}
          <div className="w-[260px] h-[480px] bg-[#0A0A0F] rounded-[36px] border-[4px] border-white/20 shadow-2xl relative p-4 flex flex-col overflow-hidden">
            {/* Phone Speaker Notch */}
            <div className="absolute top-3 left-1/2 -translate-x-1/2 w-20 h-4 bg-black rounded-full border border-white/10 flex items-center justify-center">
              <div className="w-8 h-1 bg-white/20 rounded-full" />
            </div>

            {/* Screen Header / Status Bar */}
            <div className="mt-5 flex items-center justify-between text-[10px] text-white/50 px-2 font-mono">
              <span>9:41</span>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400" />
                <span>5G</span>
              </div>
            </div>

            {/* Simulated App Banner / Notification Item Inside Phone */}
            <div className="mt-6 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <div className="w-5 h-5 rounded-md bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-[10px] font-bold">
                    G
                  </div>
                  <span className="text-[11px] font-semibold text-white/80">Ghais Quran</span>
                </div>
                <span className="text-[10px] text-white/40 font-mono">Just now</span>
              </div>

              {/* Notification Popover Card */}
              <div className="p-3.5 rounded-2xl bg-gradient-to-br from-white/[0.12] to-white/[0.04] border border-white/20 backdrop-blur-md shadow-lg space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-white tracking-tight line-clamp-1">
                    {title.trim() || 'Announcement Title'}
                  </span>
                  <span
                    className={`text-[9px] px-2 py-0.5 rounded-full font-medium ${
                      urgency === 'Urgent'
                        ? 'bg-rose-500/30 text-rose-300 border border-rose-500/40'
                        : urgency === 'Friday Reminder'
                        ? 'bg-amber-500/30 text-amber-300 border border-amber-500/40'
                        : 'bg-emerald-500/30 text-emerald-300 border border-emerald-500/40'
                    }`}
                  >
                    {urgency}
                  </span>
                </div>
                <p className="text-[11px] text-white/70 leading-snug line-clamp-4">
                  {body.trim() || 'Push notification preview text will appear here as you type your announcement message...'}
                </p>
                <div className="pt-1 flex items-center justify-between text-[10px] text-emerald-400 font-medium">
                  <span>Target: {targetTopic}</span>
                  <span>Tap to open →</span>
                </div>
              </div>
            </div>

            {/* Phone Home Bar */}
            <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-28 h-1 bg-white/30 rounded-full" />
          </div>
        </div>
      </div>

      {/* Broadcast History */}
      <div className="px-6 pb-6">
        <div className="rounded-2xl border border-white/[0.08] bg-black/20 p-4">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-medium text-white/80 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-emerald-400" />
              Broadcast History
              {!historyLoading && broadcasts.length > 0 && (
                <span className="text-[10px] text-white/40 font-mono">({broadcasts.length})</span>
              )}
            </h3>
            <button
              type="button"
              onClick={loadHistory}
              disabled={historyLoading}
              className="text-[11px] text-emerald-400 hover:text-emerald-300 disabled:opacity-50 transition-colors"
            >
              {historyLoading ? 'Loading...' : 'Refresh'}
            </button>
          </div>
          {historyLoading ? (
            <p className="text-[11px] text-white/40">Loading history...</p>
          ) : historyError ? (
            <p className="text-[11px] text-rose-300">{historyError}</p>
          ) : broadcasts.length === 0 ? (
            <p className="text-[11px] text-white/40">No broadcasts yet.</p>
          ) : (
            <ul className="space-y-2 max-h-48 overflow-y-auto">
              {broadcasts.map((b) => (
                <li
                  key={b.id}
                  className="flex items-start justify-between gap-3 p-2.5 rounded-xl bg-white/[0.03] border border-white/[0.06]"
                >
                  <div className="min-w-0">
                    <div className="text-xs font-medium text-white truncate">{b.title}</div>
                    <div className="text-[11px] text-white/50 truncate">
                      {[b.audience, b.urgency].filter(Boolean).join(' • ') || 'Broadcast'}
                    </div>
                    {b.timestamp && (
                      <div className="text-[10px] text-white/40 font-mono">
                        {new Date(b.timestamp).toLocaleString()}
                      </div>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleDelete(b.id)}
                    disabled={deletingId === b.id}
                    className="shrink-0 text-[11px] px-2.5 py-1.5 rounded-lg border border-rose-500/30 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20 disabled:opacity-50 transition-colors"
                  >
                    {deletingId === b.id ? 'Deleting...' : 'Delete'}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="p-4 px-6 border-t border-white/[0.08] bg-white/[0.02] flex items-center justify-between">
        <span className="text-xs text-white/40">
          Gateway: FCM Production (Android / iOS APNS)
        </span>
        <Button variant="glass" onClick={handleClose} className="text-xs border-white/20 text-white/80 hover:bg-white/10">
          Close
        </Button>
      </div>
    </Modal>
  );
};

export default BroadcastModal;
