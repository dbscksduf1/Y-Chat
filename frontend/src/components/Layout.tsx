type Props = {
  children: React.ReactNode;
};

function Layout({ children }: Props) {
  return (
    <div className="flex justify-center bg-gray-100 min-h-screen">

      <div className="w-[430px] bg-white flex flex-col min-h-screen shadow">

        {children}

      </div>

    </div>
  );
}

export default Layout;