import React, { useState, useMemo, useEffect, useRef } from 'react';
import {
  ColumnDef,
  ColumnFiltersState,
  SortingState,
  VisibilityState,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  Row,
  PaginationState,
} from '@tanstack/react-table';
import { Search, X, SlidersHorizontal, Inbox } from 'lucide-react';
import { cn } from '@/lib/utils';
import { TablePagination } from './TablePagination';

export interface EmptyStateConfig {
  title?: string;
  description?: string;
  icon?: React.ComponentType<{ className?: string }>;
  action?: React.ReactNode;
}

export interface DataTableProps<TData, TValue = unknown> {
  /** Column definitions conforming to TanStack Table */
  columns: ColumnDef<TData, TValue>[];
  /** Array of row data */
  data: TData[];
  /** Loading state indicator */
  isLoading?: boolean;
  /** Number of skeleton rows shown during loading */
  skeletonRowCount?: number;
  /** Specific column key to filter against; if omitted, global search filters across all columns */
  searchKey?: string;
  /** Placeholder text for search input */
  searchPlaceholder?: string;
  /** Whether to show the global search input (default: true) */
  enableGlobalFilter?: boolean;
  /** Whether to enable row selection (default: false) */
  enableRowSelection?: boolean;
  /** Callback fired whenever row selection changes */
  onRowSelectionChange?: (selectedRows: TData[]) => void;
  /** Whether to enable pagination (default: true) */
  enablePagination?: boolean;
  /** Initial rows per page (default: 10) */
  pageSize?: number;
  /** Available page size options */
  pageSizeOptions?: number[];
  /** Custom action slot rendered on the right side of the toolbar */
  toolbarContent?: React.ReactNode;
  /** Custom empty state configuration */
  emptyState?: EmptyStateConfig;
  /** Row click handler */
  onRowClick?: (row: TData) => void;
  /** Optional class name for the outer container */
  className?: string;
  /** Optional class name for the scrollable table wrapper */
  wrapperClassName?: string;
  /** Whether to display the column visibility toggle menu (default: true) */
  showColumnToggle?: boolean;
}

/** Checkbox component with indeterminate support */
function TableCheckbox({
  checked,
  indeterminate,
  onChange,
  disabled,
  ariaLabel,
}: {
  checked: boolean;
  indeterminate?: boolean;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  disabled?: boolean;
  ariaLabel: string;
}) {
  const ref = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.indeterminate = !!indeterminate;
    }
  }, [indeterminate]);

  return (
    <input
      type="checkbox"
      ref={ref}
      checked={checked}
      disabled={disabled}
      onChange={onChange}
      aria-label={ariaLabel}
      className={cn(
        'h-4 w-4 rounded border-white/20 bg-noir-surface text-ghais-emerald accent-emerald-500',
        'focus:ring-1 focus:ring-ghais-emerald/50 focus:ring-offset-0 focus:outline-none',
        'hover:border-white/40 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed transition-colors'
      )}
    />
  );
}

/** Reusable helper to generate a selection column if needed manually */
export function createSelectionColumn<TData>(): ColumnDef<TData, unknown> {
  return {
    id: '__select__',
    header: ({ table }) => (
      <div className="flex items-center justify-center">
        <TableCheckbox
          checked={table.getIsAllPageRowsSelected()}
          indeterminate={table.getIsSomePageRowsSelected()}
          onChange={table.getToggleAllPageRowsSelectedHandler()}
          ariaLabel="Select all rows"
        />
      </div>
    ),
    cell: ({ row }) => (
      <div className="flex items-center justify-center">
        <TableCheckbox
          checked={row.getIsSelected()}
          disabled={!row.getCanSelect()}
          indeterminate={row.getIsSomeSelected()}
          onChange={row.getToggleSelectedHandler()}
          ariaLabel={`Select row ${row.index + 1}`}
        />
      </div>
    ),
    enableSorting: false,
    enableHiding: false,
    size: 40,
  };
}

export function DataTable<TData, TValue = unknown>({
  columns,
  data,
  isLoading = false,
  skeletonRowCount = 5,
  searchKey,
  searchPlaceholder = 'Search records...',
  enableGlobalFilter = true,
  enableRowSelection = false,
  onRowSelectionChange,
  enablePagination = true,
  pageSize = 10,
  pageSizeOptions = [10, 20, 30, 50, 100],
  toolbarContent,
  emptyState,
  onRowClick,
  className,
  wrapperClassName,
  showColumnToggle = true,
}: DataTableProps<TData, TValue>) {
  // Table states
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
  const [rowSelection, setRowSelection] = useState({});
  const [globalFilter, setGlobalFilter] = useState('');
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize,
  });

  // Column visibility dropdown state
  const [showColumnMenu, setShowColumnMenu] = useState(false);
  const columnMenuRef = useRef<HTMLDivElement>(null);

  // Close column dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        columnMenuRef.current &&
        !columnMenuRef.current.contains(event.target as Node)
      ) {
        setShowColumnMenu(false);
      }
    }
    if (showColumnMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showColumnMenu]);

  // Compute columns with selection checkbox prepended if enabled and not already provided
  const tableColumns = useMemo(() => {
    if (!enableRowSelection) return columns;

    const hasSelection = columns.some(
      (c) => c.id === '__select__' || c.id === 'select'
    );
    if (hasSelection) return columns;

    return [createSelectionColumn<TData>(), ...columns];
  }, [columns, enableRowSelection]);

  // Initialize TanStack Table
  const table = useReactTable({
    data,
    columns: tableColumns,
    state: {
      sorting,
      columnFilters,
      columnVisibility,
      rowSelection,
      globalFilter,
      pagination,
    },
    enableRowSelection,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnVisibilityChange: setColumnVisibility,
    onGlobalFilterChange: setGlobalFilter,
    onPaginationChange: setPagination,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: enablePagination ? getPaginationRowModel() : undefined,
    getSortedRowModel: getSortedRowModel(),
    globalFilterFn: (row, _columnId, filterVal) => {
      if (!filterVal) return true;
      const search = String(filterVal).toLowerCase().trim();
      if (!search) return true;

      return row.getAllCells().some((cell) => {
        const val = cell.getValue();
        if (val == null) return false;
        return String(val).toLowerCase().includes(search);
      });
    },
  });

  // Safely notify parent when row selection changes
  const onRowSelectionChangeRef = useRef(onRowSelectionChange);
  onRowSelectionChangeRef.current = onRowSelectionChange;

  useEffect(() => {
    if (onRowSelectionChangeRef.current) {
      const selected = table
        .getSelectedRowModel()
        .rows.map((r) => r.original);
      onRowSelectionChangeRef.current(selected);
    }
  }, [rowSelection, table]);

  // Search input handler
  const handleSearchChange = (value: string) => {
    if (searchKey) {
      table.getColumn(searchKey)?.setFilterValue(value);
    } else {
      setGlobalFilter(value);
    }
  };

  const handleClearSearch = () => {
    if (searchKey) {
      table.getColumn(searchKey)?.setFilterValue('');
    } else {
      setGlobalFilter('');
    }
  };

  const currentSearchValue = searchKey
    ? (table.getColumn(searchKey)?.getFilterValue() as string) ?? ''
    : globalFilter;

  // Empty state configuration
  const EmptyIcon = emptyState?.icon || Inbox;
  const emptyTitle = emptyState?.title || 'No records found';
  const emptyDesc =
    emptyState?.description ||
    (currentSearchValue
      ? 'No results matched your search query. Try searching for something else.'
      : 'There are currently no entries to display.');

  // Shimmer skeleton bar widths for varied, natural loading appearance
  const skeletonWidths = [80, 60, 45, 90, 70, 55, 65];

  return (
    <div className={cn('w-full space-y-3.5', className)}>
      {/* Table Toolbar */}
      {(enableGlobalFilter || toolbarContent || showColumnToggle) && (
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          {/* Search Input */}
          {enableGlobalFilter && (
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500 pointer-events-none" />
              <input
                type="text"
                value={currentSearchValue}
                onChange={(e) => handleSearchChange(e.target.value)}
                placeholder={searchPlaceholder}
                aria-label={searchPlaceholder}
                className="h-9 w-full rounded-lg bg-noir-surface/90 border border-white/[0.08] pl-9 pr-8 text-xs text-zinc-200 placeholder:text-zinc-500 hover:border-white/20 focus:border-ghais-emerald/50 focus:outline-none focus:ring-1 focus:ring-ghais-emerald/50 transition-colors shadow-inner"
              />
              {currentSearchValue && (
                <button
                  type="button"
                  onClick={handleClearSearch}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors"
                  title="Clear search"
                  aria-label="Clear search"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
          )}

          {/* Right Toolbar Slot (Actions & Column Toggle) */}
          <div className="flex items-center gap-2.5 ml-auto">
            {toolbarContent}

            {/* Column Visibility Menu */}
            {showColumnToggle && (
              <div className="relative" ref={columnMenuRef}>
                <button
                  type="button"
                  onClick={() => setShowColumnMenu((prev) => !prev)}
                  className="inline-flex items-center gap-1.5 h-9 rounded-lg border border-white/[0.08] bg-white/[0.02] px-3 text-xs font-medium text-zinc-300 hover:bg-white/[0.06] hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ghais-emerald/50 cursor-pointer shadow-sm select-none"
                  title="Toggle visible columns"
                >
                  <SlidersHorizontal className="h-3.5 w-3.5 text-zinc-400" />
                  <span>Columns</span>
                </button>

                {showColumnMenu && (
                  <div className="absolute right-0 mt-2 w-52 rounded-xl border border-white/[0.1] bg-noir-elevated/95 backdrop-blur-xl p-2.5 shadow-2xl z-30 animate-in fade-in zoom-in-95 duration-150">
                    <div className="px-2 py-1 text-[11px] font-semibold uppercase tracking-wider text-zinc-400 border-b border-white/[0.06] mb-1.5">
                      Visible Columns
                    </div>
                    <div className="max-h-60 overflow-y-auto space-y-1 py-0.5">
                      {table
                        .getAllLeafColumns()
                        .filter((col) => col.getCanHide())
                        .map((col) => (
                          <label
                            key={col.id}
                            className="flex items-center gap-2 px-2 py-1.5 rounded-lg text-xs text-zinc-300 hover:bg-white/[0.05] cursor-pointer transition-colors"
                          >
                            <input
                              type="checkbox"
                              checked={col.getIsVisible()}
                              onChange={col.getToggleVisibilityHandler()}
                              className="h-3.5 w-3.5 rounded border-white/20 bg-noir-surface text-ghais-emerald accent-emerald-500 focus:ring-ghais-emerald/40 cursor-pointer"
                            />
                            <span className="truncate capitalize">
                              {typeof col.columnDef.header === 'string'
                                ? col.columnDef.header
                                : col.id}
                            </span>
                          </label>
                        ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Main Glass Table Container */}
      <div
        className={cn(
          'relative w-full rounded-xl border border-white/[0.08] bg-noir-surface/60 backdrop-blur-xl shadow-ambient-glow overflow-hidden',
          'before:absolute before:inset-x-0 before:top-0 before:h-px before:bg-gradient-to-r before:from-transparent before:via-white/20 before:to-transparent before:pointer-events-none before:z-20',
          wrapperClassName
        )}
      >
        <div className="overflow-x-auto w-full">
          <table className="w-full text-left border-collapse caption-bottom">
            {/* Table Header with Ghais Noir glass styling */}
            <thead className="sticky top-0 z-10 bg-noir-top/90 backdrop-blur-md border-b border-white/[0.08]">
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th
                      key={header.id}
                      colSpan={header.colSpan}
                      style={{ width: header.getSize() !== 150 ? header.getSize() : undefined }}
                      className="h-11 px-4 text-left align-middle text-xs font-semibold text-zinc-400 tracking-wider select-none"
                    >
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>

            {/* Table Body */}
            <tbody className="divide-y divide-white/[0.04]">
              {isLoading ? (
                /* Loading Skeleton Rows */
                Array.from({ length: skeletonRowCount }).map((_, rowIndex) => (
                  <tr
                    key={`skeleton-${rowIndex}`}
                    className="border-b border-white/[0.04] last:border-b-0"
                  >
                    {tableColumns.map((_, colIndex) => (
                      <td
                        key={`skeleton-cell-${rowIndex}-${colIndex}`}
                        className="px-4 py-3.5 align-middle"
                      >
                        <div
                          className="h-4 rounded bg-gradient-to-r from-white/[0.04] via-white/[0.08] to-white/[0.04] animate-pulse"
                          style={{
                            width: `${
                              skeletonWidths[
                                (rowIndex * 2 + colIndex) %
                                  skeletonWidths.length
                              ]
                            }%`,
                          }}
                        />
                      </td>
                    ))}
                  </tr>
                ))
              ) : table.getRowModel().rows.length > 0 ? (
                /* Data Rows */
                table.getRowModel().rows.map((row) => (
                  <tr
                    key={row.id}
                    data-state={row.getIsSelected() ? 'selected' : undefined}
                    onClick={() => onRowClick && onRowClick(row.original)}
                    className={cn(
                      'group transition-colors duration-150 border-b border-white/[0.04] last:border-b-0',
                      'hover:bg-white/[0.03]',
                      row.getIsSelected()
                        ? 'bg-ghais-emerald/[0.08] hover:bg-ghais-emerald/[0.12] border-l-2 border-l-ghais-emerald'
                        : 'even:bg-white/[0.01]',
                      onRowClick && 'cursor-pointer hover:bg-white/[0.05]'
                    )}
                  >
                    {row.getVisibleCells().map((cell) => (
                      <td
                        key={cell.id}
                        className="px-4 py-3.5 align-middle text-sm text-zinc-300 group-hover:text-zinc-100 transition-colors"
                      >
                        {flexRender(
                          cell.column.columnDef.cell,
                          cell.getContext()
                        )}
                      </td>
                    ))}
                  </tr>
                ))
              ) : (
                /* Empty State Illustration */
                <tr>
                  <td
                    colSpan={table.getVisibleLeafColumns().length || 1}
                    className="h-64 text-center align-middle"
                  >
                    <div className="flex flex-col items-center justify-center p-8 max-w-sm mx-auto text-center">
                      <div className="relative mb-3 flex items-center justify-center">
                        <div className="absolute -inset-2 rounded-full bg-ghais-emerald/5 blur-xl pointer-events-none" />
                        <div className="relative flex h-13 w-13 items-center justify-center rounded-2xl border border-white/[0.08] bg-noir-elevated/80 shadow-specular-soft text-zinc-400 p-3">
                          <EmptyIcon className="h-6 w-6 text-zinc-400 stroke-[1.5]" />
                        </div>
                      </div>
                      <h3 className="text-sm font-semibold text-zinc-200">
                        {emptyTitle}
                      </h3>
                      <p className="mt-1 text-xs text-zinc-400 max-w-xs leading-relaxed">
                        {emptyDesc}
                      </p>
                      {currentSearchValue && (
                        <button
                          type="button"
                          onClick={handleClearSearch}
                          className="mt-3.5 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-white/[0.1] bg-white/[0.04] hover:bg-white/[0.08] text-xs font-medium text-zinc-200 transition-colors cursor-pointer"
                        >
                          <X className="h-3.5 w-3.5" />
                          Clear search
                        </button>
                      )}
                      {emptyState?.action && (
                        <div className="mt-3.5">{emptyState.action}</div>
                      )}
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Bottom Pagination Bar */}
        {enablePagination && (
          <TablePagination
            table={table}
            pageSizeOptions={pageSizeOptions}
          />
        )}
      </div>
    </div>
  );
}
