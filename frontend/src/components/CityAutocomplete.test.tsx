import { render, screen, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CityAutocomplete } from './CityAutocomplete';

describe('CityAutocomplete', () => {
  it('shows matching known cities on focus and calls onChange when one is picked', () => {
    const onChange = vi.fn();
    render(<CityAutocomplete label="City" value="Lis" onChange={onChange} />);

    fireEvent.focus(screen.getByLabelText('City'));

    expect(screen.getByText('Lisbon')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Lisbon'));

    expect(onChange).toHaveBeenCalledWith('Lisbon');
  });

  it('shows no suggestions for a value that matches no known city', () => {
    render(<CityAutocomplete label="City" value="Zzzz" onChange={vi.fn()} />);

    fireEvent.focus(screen.getByLabelText('City'));

    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });

  it('forwards typed input to onChange', () => {
    const onChange = vi.fn();
    render(<CityAutocomplete label="City" value="" onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('City'), { target: { value: 'Madrid' } });

    expect(onChange).toHaveBeenCalledWith('Madrid');
  });
});
