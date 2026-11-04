import React from 'react';
import { Column } from '@tanstack/react-table';
import { ArrowDown, ArrowUp, ChevronsUpDown } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface ColumnHeaderProps<TData, TValue>
  extends Omit<React.HTMLAttributes<HTMLDivElement>, 'title'> {
  column: Column<TData, TValue>;
  title: React.ReactNode;
}

export function ColumnHeader<TData, TValue>({
  column,
  title,
  className,
}: ColumnHeaderProps<TData, TValue>) {
  if (!column.getCanSort()) {
    return (
      <div
        className={cn(
          'text-xs font-semibold uppercase tracking-wider text-zinc-400 select-none',
          className
        )}
      >
        {title}
      </div>
    );
  }

  const isSorted = column.getIsSorted();

  const handleSort = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isSorted === 'asc') {
      // Ascending -> Descending
      column.toggleSorting(true);
    } else if (isSorted === 'desc') {
      // Descending -> Clear sort
      column.clearSorting();
    } else {
      // Unsorted -> Ascending
      column.toggleSorting(false);
    }
  };

  return (
    <div className={cn('flex items-center space-x-1.5', className)}>
      <button
        type="button"
        onClick={handleSort}
        className={cn(
          'group inline-flex items-center gap-1.5 rounded-md px-2 py-1 -ml-2 text-xs font-semibold uppercase tracking-wider transition-all duration-150 select-none cursor-pointer',
          'hover:bg-white/[0.06] hover:text-zinc-100 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50',
          isSorted
            ? 'text-ghais-emerald font-bold'
            : 'text-zinc-400'
        )}
        title={
          isSorted === 'asc'
            ? 'Sorted ascending (click for descending)'
            : isSorted === 'desc'
            ? 'Sorted descending (click to clear sorting)'
            : 'Click to sort ascending'
        }
      >
        <span>{title}</span>
        <span className="flex items-center">
          {isSorted === 'desc' ? (
            <ArrowDown className="h-3.5 w-3.5 text-ghais-emerald stroke-[2.5] animate-in fade-in zoom-in duration-150" />
          ) : isSorted === 'asc' ? (
            <ArrowUp className="h-3.5 w-3.5 text-ghais-emerald stroke-[2.5] animate-in fade-in zoom-in duration-150" />
          ) : (
            <ChevronsUpDown className="h-3.5 w-3.5 text-zinc-500 opacity-60 group-hover:opacity-100 group-hover:text-zinc-300 transition-opacity" />
          )}
        </span>
      </button>
    </div>
  );
}
