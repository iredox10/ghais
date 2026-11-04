import React, { useState, useEffect } from 'react';
import { Table } from '@tanstack/react-table';
import {
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react';
import { cn } from '@/lib/utils';

export interface TablePaginationProps<TData> {
  table: Table<TData>;
  showPageJump?: boolean;
  showPageSize?: boolean;
  pageSizeOptions?: number[];
  className?: string;
}

export function TablePagination<TData>({
  table,
  showPageJump = true,
  showPageSize = true,
  pageSizeOptions = [10, 20, 30, 50, 100],
  className,
}: TablePaginationProps<TData>) {
  const pageIndex = table.getState().pagination.pageIndex;
  const pageSize = table.getState().pagination.pageSize;
  const totalRows = table.getFilteredRowModel().rows.length;
  const selectedRows = table.getFilteredSelectedRowModel().rows.length;
  const pageCount = table.getPageCount() || 1;

  const startRow = totalRows === 0 ? 0 : pageIndex * pageSize + 1;
  const endRow = Math.min((pageIndex + 1) * pageSize, totalRows);

  const [jumpPage, setJumpPage] = useState<string>(String(pageIndex + 1));

  // Sync internal state when page changes externally
  useEffect(() => {
    setJumpPage(String(pageIndex + 1));
  }, [pageIndex]);

  const handleJumpSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const pageNum = Number(jumpPage);
    if (!isNaN(pageNum) && pageNum >= 1 && pageNum <= pageCount) {
      table.setPageIndex(pageNum - 1);
    } else {
      setJumpPage(String(pageIndex + 1));
    }
  };

  return (
    <div
      className={cn(
        'flex flex-col sm:flex-row items-center justify-between gap-4 px-4 py-3 border-t border-white/[0.06] bg-noir-top/50 backdrop-blur-md text-xs text-zinc-400 select-none',
        className
      )}
    >
      {/* Left: Record Counter & Selection */}
      <div className="flex items-center gap-2 flex-wrap">
        <div>
          Showing{' '}
          <span className="font-semibold text-zinc-200">{startRow}</span> to{' '}
          <span className="font-semibold text-zinc-200">{endRow}</span> of{' '}
          <span className="font-semibold text-zinc-200">{totalRows}</span>{' '}
          records
        </div>

        {selectedRows > 0 && (
          <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-ghais-emerald/10 border border-ghais-emerald/20 text-ghais-emerald font-medium text-[11px]">
            <span className="w-1.5 h-1.5 rounded-full bg-ghais-emerald animate-pulse" />
            {selectedRows} selected
          </span>
        )}
      </div>

      {/* Right: Controls (Page Size, Jump, Navigation) */}
      <div className="flex items-center gap-4 flex-wrap justify-end">
        {/* Page Size Selector */}
        {showPageSize && (
          <div className="flex items-center gap-2">
            <span className="text-zinc-400">Rows per page:</span>
            <select
              value={pageSize}
              onChange={(e) => table.setPageSize(Number(e.target.value))}
              aria-label="Rows per page"
              className="h-8 rounded-lg bg-noir-surface border border-white/[0.08] px-2.5 py-1 text-xs text-zinc-200 font-medium hover:border-white/20 focus:border-ghais-emerald/50 focus:outline-none focus:ring-1 focus:ring-ghais-emerald/50 cursor-pointer transition-colors shadow-sm"
            >
              {pageSizeOptions.map((size) => (
                <option
                  key={size}
                  value={size}
                  className="bg-noir-elevated text-zinc-200 py-1"
                >
                  {size}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Page Jump */}
        {showPageJump && pageCount > 1 && (
          <form onSubmit={handleJumpSubmit} className="flex items-center gap-1.5">
            <span className="text-zinc-400">Go to page:</span>
            <input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={jumpPage}
              onChange={(e) => setJumpPage(e.target.value)}
              onBlur={() => {
                const pageNum = Number(jumpPage);
                if (!isNaN(pageNum) && pageNum >= 1 && pageNum <= pageCount) {
                  table.setPageIndex(pageNum - 1);
                } else {
                  setJumpPage(String(pageIndex + 1));
                }
              }}
              aria-label="Jump to page"
              className="h-8 w-12 rounded-lg bg-noir-surface border border-white/[0.08] px-2 text-center text-xs text-zinc-200 font-medium hover:border-white/20 focus:border-ghais-emerald/50 focus:outline-none focus:ring-1 focus:ring-ghais-emerald/50 transition-colors shadow-sm"
            />
            <span className="text-zinc-500">of {pageCount}</span>
          </form>
        )}

        {/* Navigation Buttons */}
        <div className="flex items-center gap-1">
          {/* First Page */}
          <button
            type="button"
            onClick={() => table.setPageIndex(0)}
            disabled={!table.getCanPreviousPage()}
            aria-label="First page"
            title="First page"
            className="h-8 w-8 inline-flex items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.02] text-zinc-400 hover:bg-white/[0.06] hover:text-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50"
          >
            <ChevronsLeft className="h-4 w-4" />
          </button>

          {/* Previous Page */}
          <button
            type="button"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
            aria-label="Previous page"
            title="Previous page"
            className="h-8 w-8 inline-flex items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.02] text-zinc-400 hover:bg-white/[0.06] hover:text-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>

          {/* Current Page Indicator */}
          <div className="px-2 text-xs font-medium text-zinc-300 min-w-[70px] text-center">
            Page {pageIndex + 1} of {pageCount}
          </div>

          {/* Next Page */}
          <button
            type="button"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
            aria-label="Next page"
            title="Next page"
            className="h-8 w-8 inline-flex items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.02] text-zinc-400 hover:bg-white/[0.06] hover:text-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50"
          >
            <ChevronRight className="h-4 w-4" />
          </button>

          {/* Last Page */}
          <button
            type="button"
            onClick={() => table.setPageIndex(pageCount - 1)}
            disabled={!table.getCanNextPage()}
            aria-label="Last page"
            title="Last page"
            className="h-8 w-8 inline-flex items-center justify-center rounded-lg border border-white/[0.08] bg-white/[0.02] text-zinc-400 hover:bg-white/[0.06] hover:text-zinc-100 disabled:opacity-30 disabled:pointer-events-none transition-all shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50"
          >
            <ChevronsRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
