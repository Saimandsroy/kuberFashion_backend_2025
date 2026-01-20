#!/usr/bin/env python3
"""
PostgreSQL Import Script.

Executes the generated pg_schema.sql and pg_data.sql files
against a PostgreSQL database using psql.
"""

import subprocess
import sys
import os
import getpass


def run_psql(host: str, port: str, user: str, database: str, password: str, sql_file: str) -> bool:
    """Runs psql to execute a SQL file."""
    print(f"\nExecuting: {sql_file}")

    env = os.environ.copy()
    env['PGPASSWORD'] = password

    cmd = [
        'psql',
        '-h', host,
        '-p', port,
        '-U', user,
        '-d', database,
        '-f', sql_file,
        '-v', 'ON_ERROR_STOP=1'  # Stop on first error
    ]

    try:
        result = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"Error executing {sql_file}:")
            print(result.stderr)
            return False
        else:
            print(f"Successfully executed {sql_file}")
            if result.stdout:
                # Print first 500 chars of output to avoid flooding
                output = result.stdout[:500]
                if len(result.stdout) > 500:
                    output += "\n... (output truncated)"
                print(output)
            return True
    except FileNotFoundError:
        print("Error: 'psql' command not found. Please ensure PostgreSQL client tools are installed and in your PATH.")
        return False
    except Exception as e:
        print(f"Unexpected error: {e}")
        return False


def main():
    print("=" * 60)
    print("PostgreSQL Database Import Script")
    print("=" * 60)

    script_dir = os.path.dirname(os.path.abspath(__file__))
    schema_file = os.path.join(script_dir, "pg_schema.sql")
    data_file = os.path.join(script_dir, "pg_data.sql")

    # Check if files exist
    if not os.path.exists(schema_file):
        print(f"Error: Schema file not found: {schema_file}")
        print("Please run convert_mysql_to_postgres.py first.")
        sys.exit(1)

    if not os.path.exists(data_file):
        print(f"Error: Data file not found: {data_file}")
        print("Please run convert_mysql_to_postgres.py first.")
        sys.exit(1)

    # Get database connection details
    print("\nEnter PostgreSQL connection details:")
    host = input("Host [localhost]: ").strip() or "localhost"
    port = input("Port [5432]: ").strip() or "5432"
    database = input("Database name: ").strip()
    if not database:
        print("Error: Database name is required.")
        sys.exit(1)
    user = input("Username [postgres]: ").strip() or "postgres"
    password = getpass.getpass("Password: ")

    print("\n" + "=" * 60)
    print("Starting import process...")
    print("=" * 60)

    # Execute schema first
    print("\n[Step 1/2] Creating tables...")
    if not run_psql(host, port, user, database, password, schema_file):
        print("\nSchema import failed. Stopping.")
        sys.exit(1)

    # Execute data
    print("\n[Step 2/2] Inserting data...")
    if not run_psql(host, port, user, database, password, data_file):
        print("\nData import failed.")
        sys.exit(1)

    print("\n" + "=" * 60)
    print("Import completed successfully!")
    print("=" * 60)


if __name__ == "__main__":
    main()
