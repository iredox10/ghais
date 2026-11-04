
import React from 'react';
import { ColumnDef } from '@tanstack/react-table';
import { DataTable, ColumnHeader } from './index';

interface ExampleUser {
  id: string;
  name: string;
  email: string;
  role: string;
}

const columns: ColumnDef<ExampleUser, any>[] = [
  {
    accessorKey: 'name',
    header: ({ column }) => <ColumnHeader column={column} title="Name" />,
    cell: ({ row }) => <span>{row.original.name}</span>,
  },
  {
    accessorKey: 'email',
    header: ({ column }) => <ColumnHeader column={column} title="Email" />,
    cell: ({ row }) => <span>{row.original.email}</span>,
  }
];

export function ExampleTable({ users }: { users: ExampleUser[] }) {
  return (
    <DataTable<ExampleUser>
      columns={columns}
      data={users}
      enableRowSelection={true}
      onRowSelectionChange={(selected: ExampleUser[]) => console.log(selected)}
      searchPlaceholder="Search users..."
    />
  );
}
