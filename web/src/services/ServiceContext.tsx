import React, { createContext, useContext, type ReactNode } from 'react';
import { SettingsRepository, settingsRepo } from '../infrastructure/SettingsRepository';
import { FontRepository, fontRepo } from '../infrastructure/FontRepository';

interface Services {
  settingsRepo: SettingsRepository;
  fontRepo: FontRepository;
}

const ServiceContext = createContext<Services | null>(null);

export const ServiceProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const services: Services = {
    settingsRepo,
    fontRepo,
  };

  return (
    <ServiceContext.Provider value={services}>
      {children}
    </ServiceContext.Provider>
  );
};

export const useServices = () => {
  const context = useContext(ServiceContext);
  if (!context) {
    throw new Error('useServices must be used within a ServiceProvider');
  }
  return context;
};
